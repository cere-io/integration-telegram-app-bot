package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.MessageId
import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("meme")
class MemeCallback(@RestClient private val botApi: BotApi) {
    @POST
    fun replyWithMeme(rq: MemeCallbackRequest) {
        val chatId = ChatId(rq.groupId.toString())
        val rq = TelegramRequest.SendPhotoRequest(
            chat_id = chatId,
            photo = rq.imageUrl,
            caption = "Here is your meme",
            reply_parameters = ReplyParameters(
                message_id = MessageId(rq.messageId),
                chat_id = chatId
            )
        )
        botApi.sendPhoto(rq)
    }
}