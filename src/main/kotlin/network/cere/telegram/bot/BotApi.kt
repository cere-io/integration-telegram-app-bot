package network.cere.telegram.bot

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.TelegramResponse
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "tg-bot-api")
interface BotApi {
    @POST
    @Path("/setWebhook")
    fun setWebhook(rq: TelegramRequest.SetWebhookRequest): TelegramResponse<Boolean>
    
    @POST
    @Path("/sendMessage")
    fun sendMessage(rq: TelegramRequest.SendMessageRequest): TelegramResponse<com.github.omarmiatello.telegram.Message>
}
