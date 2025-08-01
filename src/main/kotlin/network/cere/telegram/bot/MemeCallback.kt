package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.MessageId
import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import org.eclipse.microprofile.config.inject.ConfigProperty

@Path("meme")
class MemeCallback(
    @RestClient private val botApi: BotApi,
    @ConfigProperty(name = "ddc.bucket") private val bucket: Long,
    @ConfigProperty(name = "ddc.cdn-url") private val cdnUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @POST
    fun replyWithMeme(rq: MemeCallbackRequest) {
        val chatId = ChatId(rq.groupId.toString())

        try {
            val imageUrl = when {
                rq.imageUrl != null -> {
                    log.info("Using provided image URL: {}", rq.imageUrl)
                    rq.imageUrl
                }
                rq.imageCid != null -> {
                    val ddcUrl = "${cdnUrl}/$bucket/${rq.imageCid}/"
                    log.info("Converting CID to DDC URL: {}", ddcUrl)
                    ddcUrl
                }
                else -> throw IllegalArgumentException("Neither imageUrl nor imageCid provided")

            }

            val photoRequest = TelegramRequest.SendPhotoRequest(
                chat_id = chatId,
                photo = imageUrl,
                caption = "✅ Here is your processed meme!",
                reply_parameters = ReplyParameters(
                    message_id = MessageId(rq.messageId),
                    chat_id = chatId
                )
            )
            botApi.sendPhoto(photoRequest)
            log.info("✅ Meme sent successfully using URL")
        } catch (e: Exception) {
            log.error("Failed to send meme", e)
        }
    }


}