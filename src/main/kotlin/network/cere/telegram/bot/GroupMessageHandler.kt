package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.TelegramRequest.GetFileRequest
import com.github.omarmiatello.telegram.Update
import com.github.omarmiatello.telegram.ParseMode
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class GroupMessageHandler(
    @RestClient private val computeEngineClient: ComputeEngineClient,
    @RestClient private val cereWalletClient: CereWalletClient,
    @RestClient private val botApi: BotApi,
    @RestClient private val ruleServiceClient: RuleServiceClient,
    private val json: Json,
    private val config: Config,
    private val signer: Signer,
    @ConfigProperty(name = "telegram.webhook.url") webhookUrl: String,
    @ConfigProperty(name = "ddc.cdnUrl") cdnUrl: String,
    @ConfigProperty(name = "ddc.bucket") bucket: String,
    private val ddcService: DdcService,
    @RestClient private val botFileApi: BotFileApi,
    private val rateLimitService: RateLimitService,
    private val campaignChatCacheService: CampaignChatCacheService
) {
    private companion object {
        private const val EVENT_TYPE_MESSAGE = "TELEGRAM_MESSAGE"
        private const val EVENT_TYPE_MEME_IMAGE = "TELEGRAM_MEME_IMAGE"
        private const val MEME_HASH_TAG = "#meme"
        private const val FUN_COMMAND = "/fun"
        private const val HELP_COMMAND = "/help"
        private const val MAX_IMAGE_SIZE = 1 * 1024 * 1024
        private const val MIN_CAPTION_LENGTH = 0
        private const val MAX_LEVEL = 5
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val ddcFileUrl = "${cdnUrl}/${bucket}/"

    fun handle(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignContext = campaignChatCacheService.getCampaignContextByChatId(groupId)
        if (campaignContext == null) {
            log.warn("Channel $groupId (${chat.title}) not associated with any campaign")
            return
        }
        val from = message.from
        if (from == null) {
            log.warn("Unable to identify message author")
            return
        }

        if (!rateLimitService.canMakeChannelRequest(groupId, campaignContext.challengeSettings)) {
            log.warn("Rate limit exceeded for group $groupId")
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chat.id,
                    text = "⚠️ Too many requests from this channel. Please try again later.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chat.id
                    )
                )
            )
            return
        }
        
        rateLimitService.recordChannelRequest(groupId)

        // Handle /fun command first
        if (message.text?.startsWith(FUN_COMMAND) == true) {
            log.info("Handling /fun command from user ${from.username ?: from.id}")
            handleFunCommand(update)
            return
        }

        // Handle image response to /fun command
        if (message.photo != null && isReplyToFunCommand(message)) {
            log.info("Handling fun image response from user ${from.username ?: from.id}")
            handleFunImageResponse(update)
            return
        }
        
        if (shouldProcessImageGeneration(message)) {
            handleImageForMeme(update)
        }

        if (message.text?.startsWith(HELP_COMMAND) == true) {
            handleHelpCommand(update)
        }
        val wallet = cereWalletClient.walletByTelegramUserId(from.id.longValue).data
        val event = Event(
            payload = MessageEventPayload(
                orgId = campaignContext.orgId.toInt(),
                campaignId = campaignContext.campaignId.toString(),
                groupId = groupId,
                messageId = message.message_id.longValue,
                dateUnixTime = message.date,
                fromUserId = from.id.longValue,
                fromUserName = from.username ?: "unknown",
                text = message.text
                    ?: message.caption
                    ?: when {
                        message.photo != null -> "[Photo]"
                        message.video != null -> "[Video]"
                        message.document != null -> "[Document]"
                        message.sticker != null -> "[Sticker]"
                        else -> "[Unsupported message type]"
                    },
            ).let(json::encodeToJsonElement),
            appId = config.appId(),
            accountId = wallet.accountId,
            userPubKey = wallet.userPubKey,
            dataServicePubKey = signer.publicKey,
            signing  = byteArrayOf(0x00, 0x01, 0x00).hex(false),
            type = EVENT_TYPE_MESSAGE,
        ).sign(signer)

        runCatching {
            computeEngineClient.sendEvent(event)
            log.info("✅ Event sent successfully")
        }.onFailure {
            log.error("❌ Failed to send event", it)
        }
    }

    private fun handleImageForMeme(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId)
        
        if (campaignCtx == null) {
            log.warn("Channel $groupId not associated with any campaign")
            return
        }
        
        val from = message.from ?: return
        val userId = from.id.longValue
        
        if (!rateLimitService.canGenerateImage(userId, campaignCtx.challengeSettings)) {
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chat.id,
                    text = "You can only generate a new aura infused avatar every ${campaignCtx.challengeSettings.cooldownHours}h.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chat.id
                    )
                )
            )
            return
        }
        
        val photo = requireNotNull(update.message?.photo)
            .filter { it.file_size != null }
            .sortedByDescending { it.file_size }
            .firstOrNull { it.file_size!! <= MAX_IMAGE_SIZE }
        val caption = extractPromptFromMessage(requireNotNull(update.message))
        log.info("Image received {} {}", photo, caption)

        val replyMessageAndProcess = when {
            photo == null -> "You forget to attach an image. The command /generate can only work if you attach your avatar/pfp image following by #<aura type> . For a list of Aura filters, type /aura ." to false
            caption.length < MIN_CAPTION_LENGTH -> "You select your aura type by adding a #<aura type e.g. #fire . For a list of Aura filters, type /aura ." to false
            else -> "Your avatar is being processed... \uD83D\uDE0A\nPlease wait a few seconds..." to true
        }

        val chatId = requireNotNull(update.message?.chat?.id)
        TelegramRequest.SendMessageRequest(
            chat_id = chatId,
            text = replyMessageAndProcess.first,
            reply_parameters = ReplyParameters(
                message_id = requireNotNull(update.message?.message_id),
                chat_id = chatId
            )
        ).also(botApi::sendMessage)

        if (replyMessageAndProcess.second) {
            rateLimitService.recordImageGeneration(userId)
            
            val wallet =
                cereWalletClient.walletByTelegramUserId(requireNotNull(update.message?.from?.id?.longValue)).data

            // Download image from Telegram and upload to DDC
            val imageCid = try {
                val fileId = requireNotNull(photo?.file_id)
                log.info("Downloading image from Telegram: {}", fileId)

                val filePath = requireNotNull(botApi.getFile(GetFileRequest(fileId)).result?.file_path) {
                    "File path not found"
                }
                val imageBytes = botFileApi.download(filePath).toFile().readBytes()
                log.info("Downloaded image, size: {} bytes. Uploading to DDC...")

                ddcService.storeFile(imageBytes).also {
                    log.info("Image uploaded to DDC with CID: {}", it)
                }
            } catch (e: Exception) {
                log.error("Failed to upload image to DDC", e)
                botApi.sendMessage(
                    TelegramRequest.SendMessageRequest(
                        chat_id = chatId,
                        text = "Sorry, failed to process your image. Please try again later.",
                        reply_parameters = ReplyParameters(
                            message_id = requireNotNull(update.message?.message_id),
                            chat_id = chatId
                        )
                    )
                )
                return
            }

            val userName = update.message?.from?.username ?: "${update.message?.from?.first_name ?: ""} ${update.message?.from?.last_name ?: ""}".trim()

            val event = Event(
                payload = MemeImageEventPayload(
                    orgId = campaignCtx.orgId.toInt(),
                    campaignId = campaignCtx.campaignId.toString(),
                    groupId = requireNotNull(chatId.longValue),
                    messageId = requireNotNull(update.message?.message_id).longValue,
                    imageUrl = "$ddcFileUrl${imageCid}",
                    prompt = caption,
                    userId = requireNotNull(update.message?.from?.id).longValue,
                    userName = userName,
                    promptTags = campaignCtx.challengeSettings.promptTags
                ).let(json::encodeToJsonElement),
                appId = config.appId(),
                accountId = wallet.accountId,
                userPubKey = wallet.userPubKey,
                dataServicePubKey = signer.publicKey,
                signing = byteArrayOf(0x00, 0x01, 0x00).hex(false),
                type = EVENT_TYPE_MEME_IMAGE,
            ).sign(signer)
            runCatching {
                computeEngineClient.sendEvent(event)
                log.info("✅ Event sent successfully")
            }.onFailure {
                log.error("❌ Failed to send event", it)
            }
        }
    }

    private fun shouldProcessImageGeneration(message: com.github.omarmiatello.telegram.Message): Boolean {
        return when {
            message.photo != null && message.caption?.contains(MEME_HASH_TAG) == true -> true
            message.photo != null && message.caption?.startsWith(FUN_COMMAND) == true -> true
            else -> false
        }
    }

    private fun extractPromptFromMessage(message: com.github.omarmiatello.telegram.Message): String {
        val caption = message.caption ?: return ""
        return when {
            caption.contains(MEME_HASH_TAG) -> caption.removePrefix(MEME_HASH_TAG).trim()
            caption.startsWith(FUN_COMMAND) -> caption.removePrefix(FUN_COMMAND).trim()
            else -> caption.trim()
        }
    }

    private fun handleFunCommand(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId)
        if (campaignCtx == null) {
            log.warn("Channel $groupId not associated with any campaign")
            return
        }

        val from = message.from ?: return
        val userId = from.id.longValue

        log.info("User ${from.username ?: from.id} (ID: $userId) sent /fun command in chat $groupId")

        if (!rateLimitService.canUseFunInChannel(userId, groupId, campaignCtx.challengeSettings.cooldownHours.toLong())) {
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chat.id,
                    text = "⚠️ You can only use /fun once every ${campaignCtx.challengeSettings.cooldownHours} h in this channel.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chat.id
                    )
                )
            )
            return
        }

        // Record usage immediately to prevent multiple /fun commands
        rateLimitService.recordFunUsageInChannel(userId, groupId)
        campaignChatCacheService.saveFunCommandUserId(groupId, userId)

        val responseText = """
        🎨 **Fun Mode Activated!**
        
        Please reply to this message with a picture attachment to start the magic 🚀
        But choose wisely, you can only do this 1 time every ${campaignCtx.challengeSettings.cooldownHours}h!
    """.trimIndent()

        botApi.sendMessage(
            TelegramRequest.SendMessageRequest(
                chat_id = chat.id,
                text = responseText,
                parse_mode = ParseMode.Markdown,
                reply_parameters = ReplyParameters(
                    message_id = message.message_id,
                    chat_id = chat.id
                )
            )
        )
    }

    private fun handleHelpCommand(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId)

        if (campaignCtx == null) {
            log.warn("Channel $groupId (${chat.title}) not associated with any campaign")
            return
        }

        val chatId = chat.id

        val responseText = """
🎮 Image Challenge Commands 🎮

Available Commands:
/fun - Apply a filter to the image you attach
/help - Shows this help message

How to use the bot:
- Image Generation: type /fun and reply to the bot message by attaching a photo to generate a new image
- Cooldowns: 1 image generation every ${campaignCtx.challengeSettings.cooldownHours} h!
""".trimIndent()

        botApi.sendMessage(
            TelegramRequest.SendMessageRequest(
                chat_id = chatId,
                text = responseText,
                parse_mode = ParseMode.Markdown,
                reply_parameters = ReplyParameters(
                    message_id = message.message_id,
                    chat_id = chatId
                )
            )
        )
    }

    private fun isReplyToFunCommand(message: com.github.omarmiatello.telegram.Message): Boolean {
        val replyToMessage = message.reply_to_message ?: return false
        val isReplyToBotMessage = replyToMessage.from?.is_bot == true &&
                replyToMessage.text?.contains("Fun Mode Activated") == true
        val isReplyToFunText = replyToMessage.text == FUN_COMMAND

        val result = isReplyToBotMessage || isReplyToFunText

        return result
    }

    private fun handleFunImageResponse(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId) ?: return
        val from = message.from ?: return
        val userId = from.id.longValue

        val funUserId = campaignChatCacheService.getFunCommandUserId(groupId)
        if (funUserId == null || funUserId != userId) {
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chat.id,
                    text = "⚠️ Only the user who sent /fun can upload the image.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chat.id
                    )
                )
            )
            return
        }

        val photos = message.photo
        if (photos.isNullOrEmpty()) {
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chat.id,
                    text = "❌ No image found. Please attach a photo to your message.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chat.id
                    )
                )
            )
            return
        }

        val photo = photos
            .filter { it.file_size != null }
            .sortedByDescending { it.file_size }
            .firstOrNull { it.file_size!! <= MAX_IMAGE_SIZE }

        if (photo == null) {
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chat.id,
                    text = "❌ Image file is too large. Please use an image smaller than 1MB.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chat.id
                    )
                )
            )
            return
        }

        val confirmationMessage = botApi.sendMessage(
            TelegramRequest.SendMessageRequest(
                chat_id = chat.id,
                text = "Image received! Selecting your AI agent ... \uD83E\uDD16",
                reply_parameters = ReplyParameters(
                    message_id = message.message_id,
                    chat_id = chat.id
                )
            )
        )

        processFunImage(update, photo, confirmationMessage)

        campaignChatCacheService.clearFunCommandUserId(groupId)
    }

    private fun processFunImage(update: Update, photo: com.github.omarmiatello.telegram.PhotoSize, confirmationMessageId: String) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId)

        if (campaignCtx == null) return

        val from = message.from ?: return
        val userId = from.id.longValue

        try {
            val wallet = cereWalletClient.walletByTelegramUserId(userId).data

            val imageCid = try {
                val fileId = requireNotNull(photo.file_id)
                val filePath = requireNotNull(botApi.getFile(GetFileRequest(fileId)).result?.file_path) {
                    "File path not found"
                }
                val imageBytes = botFileApi.download(filePath).toFile().readBytes()
                ddcService.storeFile(imageBytes).also {
                    log.info("Fun image uploaded to DDC with CID: {}", it)
                }
            } catch (e: Exception) {
                log.error("Failed to upload fun image to DDC", e)
                // Rollback the fun command usage since processing failed
                rateLimitService.rollbackFunUsageInChannel(userId, groupId)
                botApi.sendMessage(
                    TelegramRequest.SendMessageRequest(
                        chat_id = chat.id,
                        text = "Sorry, failed to process your image. Please try again later.",
                        reply_parameters = ReplyParameters(
                            message_id = message.message_id,
                            chat_id = chat.id
                        )
                    )
                )
                return
            }

            val userName = from.username ?: "${from.first_name ?: ""} ${from.last_name ?: ""}".trim()

            val event = Event(
                payload = MemeImageEventPayload(
                    orgId = campaignCtx.orgId.toInt(),
                    campaignId = campaignCtx.campaignId.toString(),
                    groupId = groupId,
                    messageId = message.message_id.longValue,
                    imageUrl = "$ddcFileUrl${imageCid}",
                    prompt = "fun_filter",
                    userId = userId,
                    userName = userName,
                    promptTags = campaignCtx.challengeSettings.promptTags
                ).let(json::encodeToJsonElement),
                appId = config.appId(),
                accountId = wallet.accountId,
                userPubKey = wallet.userPubKey,
                dataServicePubKey = signer.publicKey,
                signing = byteArrayOf(0x00, 0x01, 0x00).hex(false),
                type = EVENT_TYPE_MEME_IMAGE,
            ).sign(signer)

            runCatching {
                computeEngineClient.sendEvent(event)
                botApi.sendMessage(
                    TelegramRequest.SendMessageRequest(
                        chat_id = chat.id,
                        text = "Job accepted \uD83D\uDCAA\uD83C\uDFFC Your image is being transformed as we speak \uD83D\uDC40 The result will be shared shortly \uD83D\uDD25",
                        reply_parameters = ReplyParameters(
                            message_id = message.message_id,
                            chat_id = chat.id
                        )
                    )
                )
            }.onFailure {
                log.error("❌ Failed to send fun image event", it)
                // Rollback the fun command usage since event sending failed
                rateLimitService.rollbackFunUsageInChannel(userId, groupId)
                botApi.sendMessage(
                    TelegramRequest.SendMessageRequest(
                        chat_id = chat.id,
                        text = "❌ Failed to process your fun filter. Please try again later.",
                        reply_parameters = ReplyParameters(
                            message_id = message.message_id,
                            chat_id = chat.id
                        )
                    )
                )
            }

        } catch (e: Exception) {
            log.error("Error processing fun image", e)
            // Rollback the fun command usage since processing failed
            rateLimitService.rollbackFunUsageInChannel(userId, groupId)
        }
    }
}
