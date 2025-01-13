package network.cere.telegram.bot.streaming.webhook.command.impl.group

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.webhook.BotProducer
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@RegisterRestClient(configKey = "activity-sdk-api")
interface ActivitySdkClient {
    @POST
    @Path("/event/events")
    @Consumes(MediaType.APPLICATION_JSON)
    fun sendEvent(event: JsonObject): Response
}

@ApplicationScoped
class GroupMessageHandler(
    private val botProducer: BotProducer,
    @RestClient private val activitySdkClient: ActivitySdkClient,
    @ConfigProperty(name = "activity.sdk.endpoint") private val activitySdkEndpoint: String,
    @ConfigProperty(name = "activity.debug.display-in-telegram")
    private val displayDebugInTelegram: Boolean,
    @ConfigProperty(name = "event.app.id") private val eventAppId: String,
    @ConfigProperty(name = "event.connection.id") private val eventConnectionId: String,
    @ConfigProperty(name = "event.session.id") private val eventSessionId: String,
    @ConfigProperty(name = "event.account.id") private val eventAccountId: String,
    @ConfigProperty(name = "event.signature") private val eventSignature: String,
    @ConfigProperty(name = "event.id") private val eventId: String,
    @ConfigProperty(name = "event.type") private val eventType: String,
    @ConfigProperty(name = "event.timestamp") private val eventTimestamp: String,
    @ConfigProperty(name = "event.data.service.id") private val eventDataServiceId: String,
    @ConfigProperty(name = "event.user.pub.key") private val eventUserPubKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(update: Update) {
        try {
            val message = update.message ?: return
            val chat = message.chat
            val groupId = chat.id.longValue

            // Find the channel that has this group connected
            val channel =
                Channel.find("config.connectedGroupId = ?1", groupId).firstResult()
                    ?: run {
                        log.debug("No channel found for group ID: {}", groupId)
                        return
                    }

            // Create human-readable message
            val humanReadableMessage =
                buildString {
                    append("New message in group ${chat.title}:\n\n")
                    message.from?.let { from ->
                        append("From: ${from.first_name}")
                        from.last_name?.let { append(" $it") }
                        from.username?.let { append(" (@$it)") }
                        append("\n\n")
                    }
                    append(
                        message.text
                            ?: message.caption
                            ?: when {
                                message.photo != null -> "[Photo]"
                                message.video != null -> "[Video]"
                                message.document != null -> "[Document]"
                                message.sticker != null -> "[Sticker]"
                                else -> "[Unsupported message type]"
                            },
                    )
                }

            // Create the message payload
            val payload =
                buildJsonObject {
                    putJsonObject("user") {
                        put(
                            "id",
                            message.from
                                ?.id
                                ?.longValue
                                ?.toString() ?: "unknown",
                        )
                        put("username", message.from?.username ?: "unknown")
                        put("first_name", message.from?.first_name ?: "unknown")
                        message.from?.last_name?.let { put("last_name", it) }
                    }
                    putJsonObject("message") {
                        put("id", message.message_id.toString())
                        put(
                            "timestamp",
                            message.date.let {
                                java.time.Instant
                                    .ofEpochSecond(it.toLong())
                                    .toString()
                            },
                        )
                        put("text", message.text ?: message.caption ?: "[No text content]")
                        put("chat_type", chat.type)
                        put("chat_title", chat.title)
                        put("chat_id", chat.id.longValue.toString())
                    }
                    putJsonObject("meta") {
                        put(
                            "collected_at",
                            java.time.Instant
                                .now()
                                .toString(),
                        )
                    }
                }

            // Create the event object with values from environment variables
            val event =
                buildJsonObject {
                    put("app_id", eventAppId)
                    put("connection_id", eventConnectionId)
                    put("session_id", eventSessionId)
                    put("is_debug", false)
                    put("account_id", eventAccountId)
                    put(
                        "app_pub_key",
                        "0xbb839890cc4de672a7498be0b0a991987176a52804eaef95159d954f2652ae4f",
                    )
                    put("signature", eventSignature)
                    put("id", eventId)
                    put("event_type", eventType)
                    put("timestamp", eventTimestamp)
                    put("generated", false)
                    put("payload", payload)
                }

            // Send event to Activity SDK and get status message
            val statusMessage =
                try {
                    val response = activitySdkClient.sendEvent(event)
                    "✅ Event sent successfully (status: ${response.status})"
                } catch (e: Exception) {
                    log.error("Failed to send event to Activity SDK", e)
                    "❌ Failed to send event: ${e.message}"
                }

            // Always log complete debug information to terminal
            log.info(
                """
                |=== Message Processing Debug Info ===
                |
                |Human Readable:
                |$humanReadableMessage
                |
                |DDC Event:
                |$event
                |
                |Activity SDK Endpoint:
                |$activitySdkEndpoint
                |
                |Activity SDK Status:
                |$statusMessage
                |===================================
                """.trimMargin(),
            )

            // Only send message to Telegram if debug is enabled
            if (displayDebugInTelegram) {
                val messageToSend =
                    """
                    |$humanReadableMessage
                    |
                    |
                    |DDC Event:
                    |```
                    |$event
                    |```
                    |
                    |Activity SDK Endpoint:
                    |$activitySdkEndpoint
                    |
                    |Activity SDK Status:
                    |$statusMessage
                    """.trimMargin()

                // Send the message to all users who have this channel in their context
                BotUser
                    .find("chatContextJson like ?1", "%\"channelId\":${channel.id}%")
                    .list()
                    .forEach { user ->
                        botProducer.sendTextMessage(ChatId(user.id.toString()), messageToSend)
                    }
            }
        } catch (e: Exception) {
            log.error("Error processing group message", e)
        }
    }
}
