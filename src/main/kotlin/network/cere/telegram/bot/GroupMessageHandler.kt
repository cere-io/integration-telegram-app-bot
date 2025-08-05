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
import java.net.URI

@ApplicationScoped
class GroupMessageHandler(
    @RestClient private val computeEngineClient: ComputeEngineClient,
    @RestClient private val cereWalletClient: CereWalletClient,
    @RestClient private val botApi: BotApi,
    private val json: Json,
    private val config: Config,
    private val signer: Signer,
    @ConfigProperty(name = "telegram.webhook.url") webhookUrl: String,
    private val ddcService: DdcService,
    @RestClient private val botFileApi: BotFileApi,
) {
    private companion object {
        private const val EVENT_TYPE_MESSAGE = "TELEGRAM_MESSAGE"
        private const val EVENT_TYPE_MEME_IMAGE = "TELEGRAM_MEME_IMAGE"
        private const val MEME_HASH_TAG = "#meme"
        private const val GENERATE_COMMAND = "/generate"
        private const val MAX_IMAGE_SIZE = 50 * 1024
    }

    private val log = LoggerFactory.getLogger(javaClass)

    private val groupConfigs = config.groups().entries.associate { it.value.groupId() to it.value }
    private val botFileUrl = "https://${URI.create(webhookUrl).host}/file/"

    fun handle(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val groupConfig = groupConfigs[groupId]
        if (groupConfig == null) {
            log.warn("Group {} with id {} not configured for message onboarding", chat.title, groupId)
            return
        }
        val from = message.from
        if (from == null) {
            log.warn("Unable to identify message author")
            return
        }
        if (shouldProcessImageGeneration(message)) {
            handleImageForMeme(update)
        }
        val wallet = cereWalletClient.walletByTelegramUserId(from.id.longValue).data
        val event = Event(
            payload = MessageEventPayload(
                orgId = groupConfig.orgId(),
                campaignId = groupConfig.campaignId(),
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
        val photo = requireNotNull(update.message?.photo)
            .filter { it.file_size != null }
            .sortedByDescending { it.file_size }
            .firstOrNull { it.file_size!! <= MAX_IMAGE_SIZE }
        val caption = extractPromptFromMessage(requireNotNull(update.message))
        log.info("Image received {} {}", photo, caption)

        val replyMessageAndProcess = when {
            photo == null -> "Image is too large, the limit is $MAX_IMAGE_SIZE bytes" to false
            caption.length < 3 -> "Caption is too short" to false
            else -> "Your meme is being processed... \uD83D\uDE0A\nPlease wait a few seconds..." to true
        }

        val chatId = requireNotNull(update.message?.chat?.id)
        val sender = requireNotNull(update.message?.from)
        val senderId = sender.id.longValue
        val senderName = sender.username ?: listOfNotNull(sender.first_name, sender.last_name).joinToString(" ")

        val senderInfo = SenderInfo(id = senderId, name = senderName)
        TelegramRequest.SendMessageRequest(
            chat_id = chatId,
            text = replyMessageAndProcess.first,
            reply_parameters = ReplyParameters(
                message_id = requireNotNull(update.message?.message_id),
                chat_id = chatId
            )
        ).also(botApi::sendMessage)

        if (replyMessageAndProcess.second) {
            val wallet =
                cereWalletClient.walletByTelegramUserId(requireNotNull(update.message?.from?.id?.longValue)).data
            val groupConfig = groupConfigs.getValue(chatId.longValue)

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

            val event = Event(
                payload = MemeImageEventPayload(
                    orgId = groupConfig.orgId(),
                    campaignId = groupConfig.campaignId(),
                    groupId = requireNotNull(chatId.longValue),
                    messageId = requireNotNull(update.message?.message_id).longValue,
                    imageCid = imageCid,
                    prompt = caption,
                    senderInfo = senderInfo,
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
}
