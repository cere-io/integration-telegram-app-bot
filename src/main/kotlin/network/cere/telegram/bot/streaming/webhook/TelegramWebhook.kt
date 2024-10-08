package network.cere.telegram.bot.streaming.webhook

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.json.JsonElement
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder.create
import org.jboss.resteasy.reactive.RestResponse.Status.UNAUTHORIZED
import org.jboss.resteasy.reactive.server.ServerExceptionMapper
import org.slf4j.LoggerFactory

@Path("v1/telegram/webhook")
class TelegramWebhook(
    @ConfigProperty(name = "telegram.webhook.token") private val authToken: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val AUTH_HEADER_NAME = "X-Telegram-Bot-Api-Secret-Token"
    }

    @POST
    fun handle(@RestHeader(AUTH_HEADER_NAME) auth: String, update: JsonElement) {
        require(auth == authToken)
        log.info(update.toString())
    }

    @ServerExceptionMapper
    fun mapIllegalArgumentException(e: IllegalArgumentException): RestResponse<String> {
        return create(UNAUTHORIZED, e.message.orEmpty()).build()
    }
}
