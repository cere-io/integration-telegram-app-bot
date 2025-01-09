package network.cere.telegram.bot.streaming.webhook.command.impl.group

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.json.*
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
        @ConfigProperty(name = "activity.sdk.endpoint") private val activitySdkEndpoint: String
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
            val humanReadableMessage = buildString {
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
                                }
                )
            }

            // Create the message payload
            val payload = buildJsonObject {
                put("message_id", message.message_id.toString())
                put("group_id", groupId.toString())
                put("group_title", chat.title ?: "Unknown Group")
                put(
                        "message_text",
                        message.text
                                ?: message.caption
                                        ?: when {
                                    message.photo != null -> "[Photo]"
                                    message.video != null -> "[Video]"
                                    message.document != null -> "[Document]"
                                    message.sticker != null -> "[Sticker]"
                                    else -> "[Unsupported message type]"
                                }
                )
                put("message_timestamp", message.date.toString())
                message.from?.let { from ->
                    putJsonObject("author") {
                        put("id", from.id.longValue.toString())
                        put("username", from.username ?: "unknown")
                        put("first_name", from.first_name)
                        put("last_name", from.last_name ?: "")
                        put("is_bot", from.is_bot)
                    }
                }
                message.photo?.let { photos ->
                    putJsonArray("photos") {
                        photos.forEach { photo ->
                            addJsonObject {
                                put("file_id", photo.file_id)
                                put("file_unique_id", photo.file_unique_id)
                                put("width", photo.width.toString())
                                put("height", photo.height.toString())
                                put("file_size", (photo.file_size ?: 0).toString())
                            }
                        }
                    }
                }
                message.video?.let { video ->
                    putJsonObject("video") {
                        put("file_id", video.file_id)
                        put("file_unique_id", video.file_unique_id)
                        put("width", video.width.toString())
                        put("height", video.height.toString())
                        put("duration", video.duration.toString())
                    }
                }
            }

            // Create the event object with EXACT values from the example
            val event = buildJsonObject {
                put("generated", false)
                put("is_debug", false)
                put("app_id", "2106")
                put("connection_id", "7eeea27c-59a5-4b17-b99e-a2c7ec8d7c2f")
                put("session_id", "41bed7d0-37d8-4817-93bd-9652a8d9f310")
                put("account_id", "6TRaPXDk6GKrujRjXYviAEBbkuD8ixVUD7QdCaLAUfzv4sXX")
                put(
                        "signature",
                        "0x9e017c49a105ef8976694c2c08f8c4d43f4c78e4eb09a4bc90c2f57ce572d44bf32083684e496e3ede4be69197c14e687a2b7fd45788945e3a1ab0d593188807"
                )
                put("id", "ad9b2467-94c4-4407-918c-cc5da18271bc")
                put("event_type", "GET_LEADERBOARD")
                put("timestamp", "2025-01-03T15:07:48.168Z")
                put("payload", payload)
                put("data_service_id", "2106")
                put("user_pub_key", "6TRaPXDk6GKrujRjXYviAEBbkuD8ixVUD7QdCaLAUfzv4sXX")
            }

            // Send event to Activity SDK and get status message
            val statusMessage =
                    try {
                        val response = activitySdkClient.sendEvent(event)
                        log.info("Activity SDK response: {}", response.status)
                        "✅ Event sent successfully (status: ${response.status})"
                    } catch (e: Exception) {
                        log.error("Failed to send event to Activity SDK", e)
                        "❌ Failed to send event: ${e.message}"
                    }

            // Combine both formats in a single message
            val combinedMessage =
                    """
                |$humanReadableMessage
                |
                |
                |DDC Event:
                |```
                |${event.toString()}
                |```
                |
                |Activity SDK Status:
                |$statusMessage
            """.trimMargin()

            // Send the combined message to all users who have this channel in their context
            BotUser.find("chatContextJson like ?1", "%\"channelId\":${channel.id}%")
                    .list()
                    .forEach { user ->
                        botProducer.sendTextMessage(ChatId(user.id.toString()), combinedMessage)
                    }
        } catch (e: Exception) {
            log.error("Error processing group message", e)
        }
    }
}
