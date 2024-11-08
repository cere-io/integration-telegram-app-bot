package network.cere.telegram.bot.api

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.TelegramResponse
import com.github.omarmiatello.telegram.WebhookInfo
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.json.JsonArray
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

    @POST
    @Path("/getChatAdministrators")
    fun getChatAdministrators(rq: TelegramRequest.GetChatAdministratorsRequest): TelegramResponse<JsonArray>

    @POST
    @Path("/getChatMemberCount")
    fun getChatMemberCount(rq: TelegramRequest.GetChatMemberCountRequest): TelegramResponse<Int>
}
