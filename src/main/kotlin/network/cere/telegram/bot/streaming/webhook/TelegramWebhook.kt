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
    fun handle(
            @RestHeader(AUTH_HEADER_NAME) auth: String,
            payload: JsonElement
    ): RestResponse<Unit> {
        log.info("=== WEBHOOK DEBUG ===")
        log.info("Received webhook request")
        log.info("Auth token match: {}", auth == authToken)

        if (auth != authToken) {
            log.warn("Unauthorized webhook request - token mismatch")
            return RestResponse.status(UNAUTHORIZED)
        }

        val payloadJson = payload.toString()
        log.info("Webhook payload: {}", payloadJson)

        try {
            val update = Update.fromJson(payloadJson)
            log.info("Successfully parsed update:")
            log.info("Update ID: {}", update.update_id)
            log.info(
                    "Message Type: {}",
                    when {
                        update.message != null -> "message"
                        update.edited_message != null -> "edited_message"
                        update.channel_post != null -> "channel_post"
                        update.callback_query != null -> "callback_query"
                        else -> "unknown"
                    }
            )
            log.info("===================")

            commands.tryHandle(update)
            return RestResponse.ok()
        } catch (e: Exception) {
            log.error("Failed to process webhook update", e)
            return RestResponse.serverError()
        }
    }
}
