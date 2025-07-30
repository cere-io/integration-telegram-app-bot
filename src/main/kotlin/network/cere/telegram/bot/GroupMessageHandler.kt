package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.TelegramRequest.GetFileRequest
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import java.net.URI

@ApplicationScoped
class GroupMessageHandler(
    @RestClient private val computeEngineClient: ComputeEngineClient,
    @RestClient private val cereWalletClient: CereWalletClient,
    @RestClient private val botApi: BotApi,
    @RestClient private val botFileApi: BotFileApi,
    private val config: Config,
    private val signer: Signer,
    private val ddcService: DdcService,
    @ConfigProperty(name = "telegram.webhook.url") webhookUrl: String,
) {
    private companion object {
        private const val EVENT_TYPE_MESSAGE = "TELEGRAM_MESSAGE"
        private const val EVENT_TYPE_MEME_IMAGE = "TELEGRAM_MEME_IMAGE"
        private const val MEME_HASH_TAG = "#meme"
        private const val MAX_IMAGE_SIZE = 50 * 1024
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = jacksonObjectMapper()

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
        if (message.photo != null && message.caption?.contains(MEME_HASH_TAG) ?: false) {
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
            ).let { objectMapper.valueToTree(it) },
            appId = config.appId(),
            accountId = wallet.accountId,
            address = wallet.accountId,
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
        val photo = update.message?.photo
            ?.filter { it.file_size != null }
            ?.sortedByDescending { it.file_size }
            ?.firstOrNull { it.file_size!! <= MAX_IMAGE_SIZE }
        val caption = requireNotNull(update.message?.caption).removePrefix(MEME_HASH_TAG).trim()
        log.info("Image received {} {}", photo, caption)

        val replyMessageAndProcess = when {
            photo == null -> "Image is too large, the limit is $MAX_IMAGE_SIZE bytes" to false
            caption.length < 8 -> "Caption is too short" to false
            else -> "Your meme is being processed... \uD83D\uDE0A\nPlease wait a few seconds..." to true
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
            val wallet =
                cereWalletClient.walletByTelegramUserId(requireNotNull(update.message?.from?.id?.longValue)).data
            val groupConfig = groupConfigs.getValue(chatId.longValue)
            
            // Download image from Telegram and upload to DDC
            val imageCid = try {
                val fileId = requireNotNull(photo?.file_id)
                log.info("Downloading image from Telegram: {}", fileId)
                
                // Get file info and download
                val fileResponse = botApi.getFile(GetFileRequest(fileId))
                val filePath = requireNotNull(fileResponse.result?.file_path) { "File path not found" }
                val imageFile = botFileApi.download(filePath)
                val imageBytes = imageFile.toFile().readBytes()
                
                log.info("Downloaded image, size: {} bytes. Uploading to DDC...", imageBytes.size)
                
                // Store in DDC
                val cid = ddcService.storeFile(imageBytes)
                log.info("Image uploaded to DDC with CID: {}", cid)
                cid
            } catch (e: Exception) {
                log.error("Failed to upload image to DDC", e)
                // Send error message to user
                TelegramRequest.SendMessageRequest(
                    chat_id = chatId,
                    text = "Sorry, failed to process your image. Please try again later.",
                    reply_parameters = ReplyParameters(
                        message_id = requireNotNull(update.message?.message_id),
                        chat_id = chatId
                    )
                ).also(botApi::sendMessage)
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
                ).let { objectMapper.valueToTree(it) },
                appId = config.appId(),
                accountId = wallet.accountId,
                address = wallet.accountId,
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
}
