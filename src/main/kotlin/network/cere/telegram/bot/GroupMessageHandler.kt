package network.cere.telegram.bot

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.json.*
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.random.Random

@RegisterRestClient(configKey = "activity-sdk-api")
interface ActivitySdkClient {
    @POST
    @Path("/event/events")
    @Consumes(MediaType.APPLICATION_JSON)
    fun sendEvent(event: JsonObject): Response
}

@ApplicationScoped
class GroupMessageHandler(
    @RestClient private val activitySdkClient: ActivitySdkClient,
    @ConfigProperty(name = "event.app.id") private val eventAppId: String,
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
                            },
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

            val accountId = message.from?.id?.longValue?.let { userId ->
                val sk = ByteArray(32)
                Random(userId).nextBytes(sk)
                val pubKey = Ed25519PrivateKeyParameters(sk).generatePublicKey().encoded
                "0x" + pubKey.joinToString("") { "%02x".format(it) }
            } ?: eventAccountId

            // Create the event object with values from environment variables
            val event =
                buildJsonObject {
                    put("generated", false)
                    put("is_debug", false)
                    put("app_id", eventAppId)
                    put("connection_id", UUID.randomUUID().toString())
                    put("session_id", UUID.randomUUID().toString())
                    put("account_id", accountId)
                    put("signature", eventSignature)
                    put("id", eventId)
                    put("event_type", eventType)
                    put("timestamp", eventTimestamp)
                    put("payload", payload)
                    put("data_service_id", eventDataServiceId)
                    put("user_pub_key", eventUserPubKey)
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
                |Activity SDK Status:
                |$statusMessage
                |===================================
                """.trimMargin(),
            )
        } catch (e: Exception) {
            log.error("Error processing group message", e)
        }
    }
}
