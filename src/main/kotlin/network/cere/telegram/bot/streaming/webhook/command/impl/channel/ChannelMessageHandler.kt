package network.cere.telegram.bot.streaming.webhook.command.impl.channel

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

@ApplicationScoped
class ChannelMessageHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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

            val timestamp = LocalDateTime.now().format(dateFormatter)
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
            val from = message.from?.let { " from ${it.first_name} (ID: ${it.id.longValue})" } ?: ""

            log.info(
                    """
                |
                |==================== CHANNEL MESSAGE ====================
                |Timestamp: {}
                |Channel: {} ({})
                |Message ID: {}{}
                |Content:
                |{}
                |====================================================
                |""".trimMargin(),
                    timestamp,
                    channelTitle,
                    channelId,
                    messageId,
                    from,
                    messageText
            )
        } catch (e: Exception) {
            log.error("Error processing channel message", e)
        }
    }
}
