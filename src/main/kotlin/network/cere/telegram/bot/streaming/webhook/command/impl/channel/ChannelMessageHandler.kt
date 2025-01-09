package network.cere.telegram.bot.streaming.webhook.command.impl.channel

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.*
import network.cere.telegram.bot.streaming.webhook.BotProducer
import org.slf4j.LoggerFactory

@ApplicationScoped
class ChannelMessageHandler(private val botProducer: BotProducer) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(update: Update) {
        try {
            log.debug("Received update: {}", update.toJson())

            val message =
                    update.message
                            ?: run {
                                log.warn("No message in update")
                                return
                            }
            val chat = message.chat

            // Only process channel messages
            if (chat.type != "channel") {
                log.debug("Ignoring non-channel message from chat type: {}", chat.type)
                return
            }

            val messageText =
                    message.text
                            ?: message.caption
                                    ?: run {
                                log.debug(
                                        "Message has no text or caption, message type: {}",
                                        when {
                                            message.photo != null -> "photo"
                                            message.video != null -> "video"
                                            message.document != null -> "document"
                                            message.sticker != null -> "sticker"
                                            else -> "unknown"
                                        }
                                )
                                return
                            }

            val channelTitle = chat.title ?: "Unknown Channel"
            val channelId = chat.id.longValue
            val messageId = message.message_id
            val from = message.from

            // Create the message payload
            val payload = buildJsonObject {
                put("message_id", messageId.toString())
                put("channel_id", channelId.toString())
                put("channel_title", channelTitle)
                put("message_text", messageText)
                put("message_timestamp", message.date.toString())
                putJsonObject("author") {
                    put("id", from?.id?.longValue?.toString() ?: "unknown")
                    put("username", from?.username ?: "unknown")
                    put("first_name", from?.first_name ?: "unknown")
                    put("last_name", from?.last_name ?: "unknown")
                    put("is_bot", from?.is_bot ?: false)
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

            // Create the final event object with EXACT values from the example
            val event = buildJsonObject {
                put("app_id", "2106")
                put("connection_id", "a3c538ed-03e7-46ff-9f43-48caa79b2fc4")
                put("session_id", "9936365c-d39f-4de6-85f5-bb9ecb8c6941")
                put("is_debug", false)
                put(
                        "account_id",
                        "0xbb839890cc4de672a7498be0b0a991987176a52804eaef95159d954f2652ae4f"
                )
                put(
                        "app_pub_key",
                        "0xbb839890cc4de672a7498be0b0a991987176a52804eaef95159d954f2652ae4f"
                )
                put(
                        "signature",
                        "0xd13c27e63b419963f9bae38c40ce59c91102419282135bf034ee2ef9bbbfb4350510fe163c7c3d38422e809f349b8cb1c197d87b46ac457465d042e185e3600f"
                )
                put("id", "2a4255cb-5d56-4140-bdd5-6ad7a2fcd9fe")
                put("event_type", "X_REPOST")
                put("timestamp", "2024-12-04T11:49:45.713Z")
                put("generated", false)
                put("payload", payload)
            }

            // Send the JSON as a bot reply
            botProducer.sendTextMessage(chat.id, event.toString())
        } catch (e: Exception) {
            log.error("Error processing channel message", e)
        }
    }
}
