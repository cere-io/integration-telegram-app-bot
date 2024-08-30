package network.cere.telegram.bot.api

import com.github.omarmiatello.telegram.*
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.json.JsonElement
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "tg-bot-api")
interface BotApi {
    @GET
    @Path("/getWebhookInfo")
    fun getWebhookInfo(): TelegramResponse<WebhookInfo>

    @POST
    @Path("/setWebhook")
    fun setWebhook(rq: TelegramRequest.SetWebhookRequest): TelegramResponse<Boolean>

    @POST
    @Path("/sendMessage")
    fun sendMessage(message: TelegramRequest.SendMessageRequest)

    @POST
    @Path("/getChat")
    fun getChat(rq: TelegramRequest.GetChatRequest): TelegramResponse<JsonElement>
}