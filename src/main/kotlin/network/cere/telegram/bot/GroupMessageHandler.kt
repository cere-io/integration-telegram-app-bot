package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.TelegramRequest.GetFileRequest
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import java.time.Instant

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
        private const val EVENT_TYPE_BOOST = "BOOST_EVENT"
        private const val MEME_HASH_TAG = "#meme"
        private const val GENERATE_COMMAND = "/generate"
        private const val AVATAR_COMMAND = "/avatar"
        private const val BOOST_COMMAND = "/boost"
        private const val MAX_IMAGE_SIZE = 1 * 1024 * 1024
        private const val MIN_CAPTION_LENGTH = 3
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
        
        if (shouldProcessImageGeneration(message)) {
            handleImageForMeme(update)
        }
        
        if (message.text?.startsWith(AVATAR_COMMAND) == true) {
            handleAvatarCommand(update)
        }

        if (message.text?.startsWith(BOOST_COMMAND) == true) {
            handleBoostCommand(update)
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
                    userName = userName
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
            message.photo != null && message.caption?.startsWith(GENERATE_COMMAND) == true -> true
            else -> false
        }
    }

    private fun extractPromptFromMessage(message: com.github.omarmiatello.telegram.Message): String {
        val caption = message.caption ?: return ""
        return when {
            caption.contains(MEME_HASH_TAG) -> caption.removePrefix(MEME_HASH_TAG).trim()
            caption.startsWith(GENERATE_COMMAND) -> caption.removePrefix(GENERATE_COMMAND).trim()
            else -> caption.trim()
        }
    }
    
    private fun handleAvatarCommand(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId)

        if (campaignCtx == null) {
            log.warn("Channel $groupId (${chat.title}) not associated with any campaign")
            return
        }

        val from = message.from ?: return
        
        val chatId = chat.id
        
        botApi.sendMessage(
            TelegramRequest.SendMessageRequest(
                chat_id = chatId,
                text = "Loading your avatar... ⏳",
                reply_parameters = ReplyParameters(
                    message_id = message.message_id,
                    chat_id = chatId
                )
            )
        )
        
        try {
            val avatarParams = AvatarParams(
                userId = from.id.longValue.toString(),
                orgId = campaignCtx.orgId.toString(),
                campaignId = campaignCtx.campaignId.toString()
            )

            val avatarWrapper = AvatarRequestWrapper(params = avatarParams)
            
            val avatarResponse = ruleServiceClient.getAvatar(config.appId(), avatarWrapper)
            
            if (avatarResponse.result.code == "SUCCESS" && avatarResponse.result.data?.success == true) {
                val avatarInfo = avatarResponse.result.data.data

                avatarInfo?.url?.let {
                    botApi.sendPhoto(
                        TelegramRequest.SendPhotoRequest(
                            chat_id = chatId,
                            photo = it,
                            caption = avatarInfo.caption,
                            reply_parameters = ReplyParameters(
                                message_id = message.message_id,
                                chat_id = chatId
                            )
                        )
                    )
                }
                
                log.info("✅ Avatar sent successfully for user ${from.id}")
            } else {
                botApi.sendMessage(
                    TelegramRequest.SendMessageRequest(
                        chat_id = chatId,
                        text = "❌ Failed to get avatar. Please try again later.",
                        reply_parameters = ReplyParameters(
                            message_id = message.message_id,
                            chat_id = chatId
                        )
                    )
                )
                log.error("❌ Failed to get avatar for user ${from.id}: ${avatarResponse.result.code}")
            }
        } catch (e: Exception) {
            log.error("❌ Error while getting avatar for user ${from.id}", e)
            
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chatId,
                    text = "❌ There was an error getting your avatar. Please try again later.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chatId
                    )
                )
            )
        }
    }

    private fun handleBoostCommand(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val campaignCtx = campaignChatCacheService.getCampaignContextByChatId(groupId)

        if (campaignCtx == null) {
            log.warn("Channel $groupId (${chat.title}) not associated with any campaign")
            return
        }

        val from = message.from ?: return
        val userId = from.id.longValue

        val chatId = chat.id

        botApi.sendMessage(
            TelegramRequest.SendMessageRequest(
                chat_id = chatId,
                text = "Processing your boost request... ⏳",
                reply_parameters = ReplyParameters(
                    message_id = message.message_id,
                    chat_id = chatId
                )
            )
        )

        try {
            val avatarParams = AvatarParams(
                userId = from.id.longValue.toString(),
                orgId = campaignCtx.orgId.toString(),
                campaignId = campaignCtx.campaignId.toString()
            )

            val avatarWrapper = AvatarRequestWrapper(params = avatarParams)
            val avatarResponse = ruleServiceClient.getAvatar(config.appId(), avatarWrapper)
            
            if (avatarResponse.result.code == "SUCCESS" && avatarResponse.result.data?.success == true) {
                val avatarInfo = avatarResponse.result.data.data
                val lastBoostAt = avatarInfo?.last_boost_at
                val level = avatarInfo?.level ?: 0
                val now = Instant.now()
                val cooldownHours = campaignCtx.challengeSettings.cooldownHours.toLong()

                log.info("Boost check for user $userId: lastBoostAt=$lastBoostAt, level=$level, cooldownHours=$cooldownHours")

                val canBoost = when {
                    level >= MAX_LEVEL -> {
                        log.info("User $userId reached max level $level")
                        botApi.sendMessage(
                            TelegramRequest.SendMessageRequest(
                                chat_id = chatId,
                                text = "Congratulations! You have reached a state of enlightenment by reaching aura level 5! To remain at level, you'll have to continue to meditate daily. If you miss a day, your aura will reset!",
                                reply_parameters = ReplyParameters(
                                    message_id = message.message_id,
                                    chat_id = chatId
                                )
                            )
                        )
                        false
                    }
                    else -> {
                        if (!rateLimitService.canBoost(userId, campaignCtx.challengeSettings)) {
                            log.info("User $userId rate limit exceeded for boost")
                            botApi.sendMessage(
                                TelegramRequest.SendMessageRequest(
                                    chat_id = chatId,
                                    text = "You can only meditate and boost your aura to the next level every 24h. The maximum level is 5.",
                                    reply_parameters = ReplyParameters(
                                        message_id = message.message_id,
                                        chat_id = chatId
                                    )
                                )
                            )
                            false
                        } else {
                            log.info("User $userId can boost")
                            true
                        }
                    }
                }

                if (canBoost) {
                    rateLimitService.recordBoost(userId)
                    
                    val wallet = cereWalletClient.walletByTelegramUserId(from.id.longValue).data
                    val userName = from.username ?: "${from.first_name ?: ""} ${from.last_name ?: ""}".trim()

                    val event = Event(
                        payload = BoostEventPayload(
                            orgId = campaignCtx.orgId.toInt(),
                            campaignId = campaignCtx.campaignId.toString(),
                            groupId = groupId,
                            messageId = message.message_id.longValue,
                            userId = from.id.longValue,
                            userName = userName
                        ).let(json::encodeToJsonElement),
                        appId = config.appId(),
                        accountId = wallet.accountId,
                        userPubKey = wallet.userPubKey,
                        dataServicePubKey = signer.publicKey,
                        signing = byteArrayOf(0x00, 0x01, 0x00).hex(false),
                        type = EVENT_TYPE_BOOST,
                    ).sign(signer)

                    runCatching {
                        computeEngineClient.sendEvent(event)
                        log.info("✅ Boost event sent successfully for user ${from.id}")

                        botApi.sendMessage(
                            TelegramRequest.SendMessageRequest(
                                chat_id = chatId,
                                text = "🚀 Boost event sent! Your level will be updated shortly.",
                                reply_parameters = ReplyParameters(
                                    message_id = message.message_id,
                                    chat_id = chatId
                                )
                            )
                        )
                    }.onFailure {
                        log.error("❌ Failed to send boost event for user ${from.id}", it)

                        botApi.sendMessage(
                            TelegramRequest.SendMessageRequest(
                                chat_id = chatId,
                                text = "❌ Failed to process boost. Please try again later.",
                                reply_parameters = ReplyParameters(
                                    message_id = message.message_id,
                                    chat_id = chatId
                                )
                            )
                        )
                    }
                }
            } else {
                botApi.sendMessage(
                    TelegramRequest.SendMessageRequest(
                        chat_id = chatId,
                        text = "❌ Failed to get avatar data. Please try again later.",
                        reply_parameters = ReplyParameters(
                            message_id = message.message_id,
                            chat_id = chatId
                        )
                    )
                )
                log.error("❌ Failed to get avatar for user ${from.id}: ${avatarResponse.result.code}")
            }
        } catch (e: Exception) {
            log.error("❌ Error while processing boost for user ${from.id}", e)
            
            botApi.sendMessage(
                TelegramRequest.SendMessageRequest(
                    chat_id = chatId,
                    text = "❌ There was an error processing your boost. Please try again later.",
                    reply_parameters = ReplyParameters(
                        message_id = message.message_id,
                        chat_id = chatId
                    )
                )
            )
        }
    }
}
