package network.cere.telegram.bot

import com.github.omarmiatello.telegram.Update
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.json.JsonElement
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.RestResponse.Status.UNAUTHORIZED
import org.slf4j.LoggerFactory

@Path("telegram/webhook")
class TelegramWebhook(
    @ConfigProperty(name = "telegram.webhook.token") private val authToken: String,
    private val groupMessageHandler: GroupMessageHandler,
    private val privateMessageHandler: PrivateMessageHandler,
) {
    companion object {
        const val AUTH_HEADER_NAME = "X-Telegram-Bot-Api-Secret-Token"
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val handledTypes = setOf("group", "supergroup")

    @POST
    @RunOnVirtualThread
    fun handle(
        @RestHeader(AUTH_HEADER_NAME) auth: String,
        payload: JsonElement,
    ): RestResponse<Unit> {
        if (auth != authToken) {
            log.warn("Unauthorized webhook request - token mismatch")
            return RestResponse.status(UNAUTHORIZED)
        }

        val payloadJson = payload.toString()
        log.info("Webhook payload: {}", payloadJson)

        runCatching {
            val update = Update.fromJson(payloadJson)
            val type = update.message?.chat?.type
            when (type) {
                "private" -> {
                    log.info("Processing private message")
                    privateMessageHandler.handle(update)
                }
                in handledTypes -> {
                    val message = update.message
                    if (message?.text?.startsWith("/") == true ||
                        message?.reply_to_message?.from?.is_bot == true
                    ) {
                        // skip
                    } else {
                        log.info("Message doesn't match processing criteria - privacy mode active")
                        groupMessageHandler.handle(update)
                    }
                }
                else -> {
                    log.debug("Ignoring message type: $type")
                }
            }
        }.onFailure { log.error("Error on processing", it) }


        return RestResponse.ok()
    }
}
