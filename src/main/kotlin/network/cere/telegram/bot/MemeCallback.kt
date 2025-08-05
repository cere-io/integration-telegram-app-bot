package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.MessageId
import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import io.cere.etl.payload
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import org.eclipse.microprofile.config.inject.ConfigProperty

@Path("meme")
class MemeCallback(
    @RestClient private val botApi: BotApi,
    @ConfigProperty(name = "ddc.bucket") private val bucket: Long,
    @ConfigProperty(name = "ddc.cdn-url") private val cdnUrl: String,
    @ConfigProperty(name = "telegram.bot.token") private val botToken: String,
    private val ddcService: DdcService,
    private val config: Config,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val groupConfigs = config.groups().entries.associate { it.value.groupId() to it.value }

    @POST
    fun replyWithMeme(rq: MemeCallbackRequest) = try {
        val message = rq.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        when {
            rq.imageUrl != null     -> sendPhoto(rq.groupId, rq.messageId, rq.imageUrl!!)
            rq.imageCid != null     -> sendPhoto(rq.groupId, rq.messageId, "$cdnUrl/$bucket/${rq.imageCid}/")
            rq.imageBase64 != null  -> {
                val imageBytes = java.util.Base64.getDecoder().decode(rq.imageBase64)

                val cid = ddcService.storeFile(imageBytes)

                val imageUrl = "$cdnUrl/$bucket/$cid/"

                sendPhoto(rq.groupId, rq.messageId, imageUrl)

                val groupConfig = groupConfigs[groupId]

                val event = Event(
                    payload = MessageEventPayload(
                        orgId = groupConfig.orgId(),
                    )
                )
            }
            else -> error("No image provided")
        }
    } catch (e: Exception) {
        log.error("Failed to send meme", e)
    }

    private fun sendPhoto(groupId: Long, messageId: Long, url: String) {
        botApi.sendPhoto(
            TelegramRequest.SendPhotoRequest(
                chat_id = ChatId(groupId.toString()),
                photo = url,
                caption = "✅ Here is your processed meme!",
                reply_parameters = ReplyParameters(
                    chat_id = ChatId(groupId.toString()),
                    message_id = MessageId(messageId)
                )
            )
        )
        log.info("✅ Meme sent: $url")
    }
}