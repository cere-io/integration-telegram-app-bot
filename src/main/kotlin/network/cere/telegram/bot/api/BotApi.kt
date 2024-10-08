package network.cere.telegram.bot.api

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.TelegramResponse
import com.github.omarmiatello.telegram.WebhookInfo
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "tg-bot-api")
interface BotApi {
    @GET
    @Path("/getWebhookInfo")
    fun getWebhookInfo(): TelegramResponse<WebhookInfo>

    @POST
    @Path("/setWebhook")
    fun setWebhook(rq: TelegramRequest.SetWebhookRequest): TelegramResponse<Boolean>
}