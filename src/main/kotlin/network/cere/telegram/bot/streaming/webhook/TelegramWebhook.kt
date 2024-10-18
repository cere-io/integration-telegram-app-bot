package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.Update
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.json.JsonElement
import network.cere.telegram.bot.streaming.webhook.command.BotCommands
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.RestResponse.Status.UNAUTHORIZED
import org.slf4j.LoggerFactory

@Path("telegram/webhook")
class TelegramWebhook(
    @ConfigProperty(name = "telegram.webhook.token") private val authToken: String,
    private val commands: BotCommands,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val AUTH_HEADER_NAME = "X-Telegram-Bot-Api-Secret-Token"
    }

    @POST
    @RunOnVirtualThread
    @Transactional
    fun handle(@RestHeader(AUTH_HEADER_NAME) auth: String, payload: JsonElement): RestResponse<Unit> {
        if (auth != authToken) {
            return RestResponse.status(UNAUTHORIZED)
        }
        val payloadJson = payload.toString()
        log.debug("Received update from Telegram: {}", payloadJson)
        val update = Update.fromJson(payloadJson)
        commands.tryHandle(update)
        return RestResponse.ok()
    }
}
