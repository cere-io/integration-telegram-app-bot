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
    private val groupSubscriptionTracker: GroupSubscriptionTracker,
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
            
            // Handle my_chat_member updates (bot added/removed from groups)
            update.my_chat_member?.let { chatMember ->
                val chat = chatMember.chat
                val newStatus = chatMember.new_chat_member.status
                val oldStatus = chatMember.old_chat_member.status
                
                log.info("TelegramWebhook.kt: Bot membership changed in {} ({}): {} → {}", 
                    chat.id.longValue, chat.title, oldStatus, newStatus)
                
                when (newStatus) {
                    "member", "administrator" -> {
                        if (chat.type in handledTypes) {
                            groupSubscriptionTracker.addGroup(chat.id.longValue, chat.title)
                        }
                    }
                    "left", "kicked" -> {
                        groupSubscriptionTracker.removeGroup(chat.id.longValue, chat.title)
                    }
                }
                return RestResponse.ok()
            }
            
            // Handle regular messages
            val message = update.message
            val chat = message?.chat
            val type = chat?.type
            
            when (type) {
                "private" -> {
                    log.info("TelegramWebhook.kt: Processing private message")
                    privateMessageHandler.handle(update)
                }
                in handledTypes -> {
                    // Track group subscription dynamically when we receive messages
                    if (chat != null) {
                        groupSubscriptionTracker.addGroup(chat.id.longValue, chat.title)
                    }
                    
                    if (message?.text?.startsWith("/") == true ||
                        message?.reply_to_message?.from?.is_bot == true
                    ) {
                        log.debug("TelegramWebhook.kt: Skipping command or bot reply message")
                    } else {
                        log.info("TelegramWebhook.kt: Processing group message from tracked group")
                        groupMessageHandler.handle(update)
                    }
                }
                else -> {
                    log.debug("TelegramWebhook.kt: Ignoring message type: {}", type)
                }
            }
        }.onFailure { log.error("TelegramWebhook.kt: Error on processing", it) }


        return RestResponse.ok()
    }
}
