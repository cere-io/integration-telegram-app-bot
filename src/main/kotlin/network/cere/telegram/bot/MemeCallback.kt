package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.MessageId
import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Path("meme")
class MemeCallback(
    @RestClient private val botApi: BotApi,
    @ConfigProperty(name = "ddc.bucket") private val bucket: Long,
    @ConfigProperty(name = "ddc.cdn-url") private val cdnUrl: String,
    @ConfigProperty(name = "telegram.bot.token") private val botToken: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @POST
    fun replyWithMeme(rq: MemeCallbackRequest) {
        val chatId = ChatId(rq.groupId.toString())

        try {
            when {
                rq.imageUrl != null -> {
                    log.info("Using provided image URL: {}", rq.imageUrl)
                    sendPhotoWithUrl(chatId, rq.messageId, rq.imageUrl!!)
                }
                rq.imageCid != null -> {
                    val ddcUrl = "${cdnUrl}/$bucket/${rq.imageCid}/"
                    log.info("Converting CID to DDC URL: {}", ddcUrl)
                    sendPhotoWithUrl(chatId, rq.messageId, ddcUrl)
                }
                rq.imageBase64 != null -> {
                    log.info("Processing image base64, length: {}", rq.imageBase64!!.length)
                    // Decode base64 to ByteArray
                    val bytes = java.util.Base64.getDecoder().decode(rq.imageBase64!!)
                    
                    // Send bytes directly to Telegram
                    sendPhotoWithBytes(rq.groupId, rq.messageId, bytes)
                }
                else -> throw IllegalArgumentException("Neither imageUrl, imageCid, nor imageBase64 provided")
            }
        } catch (e: Exception) {
            log.error("Failed to send meme", e)
        }
    }

    private fun sendPhotoWithUrl(chatId: ChatId, messageId: Long, imageUrl: String) {
        val photoRequest = TelegramRequest.SendPhotoRequest(
            chat_id = chatId,
            photo = imageUrl,
            caption = "✅ Here is your processed meme!",
            reply_parameters = ReplyParameters(
                message_id = MessageId(messageId),
                chat_id = chatId
            )
        )
        botApi.sendPhoto(photoRequest)
        log.info("✅ Meme sent successfully using URL")
    }

    private fun sendPhotoWithBytes(groupId: Long, messageId: Long, imageBytes: ByteArray) {
        log.info("Attempting to send photo with bytes, size: {}", imageBytes.size)
        
        try {
            // Create multipart form data with binary data
            val boundary = "boundary"
            val multipartData = buildString {
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
                append("$groupId\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"reply_to_message_id\"\r\n\r\n")
                append("$messageId\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
                append("✅ Here is your processed meme!\r\n")
                append("--$boundary\r\n")
                append("Content-Disposition: form-data; name=\"photo\"; filename=\"image.png\"\r\n")
                append("Content-Type: image/png\r\n\r\n")
            }
            
            // Create binary multipart data
            val boundaryBytes = "--$boundary\r\n".toByteArray()
            val endBoundaryBytes = "\r\n--$boundary--\r\n".toByteArray()
            
            val multipartBytes = multipartData.toByteArray() + imageBytes + endBoundaryBytes
            
            // Use HTTP client directly for multipart upload
            val client = java.net.http.HttpClient.newHttpClient()
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.telegram.org/bot$botToken/sendPhoto"))
                .header("Content-Type", "multipart/form-data; boundary=$boundary")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(multipartBytes))
                .build()
            
            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
            
            if (response.statusCode() == 200) {
                log.info("✅ Photo sent successfully with bytes")
            } else {
                throw Exception("Failed to send photo: ${response.body()}")
            }
            
        } catch (e: Exception) {
            log.error("Failed to send photo with bytes, falling back to message", e)
            
            // Fallback: send message with image information
            val messageRequest = TelegramRequest.SendMessageRequest(
                chat_id = ChatId(groupId.toString()),
                text = """
                    📸 **Image Received**
                    
                    Size: ${formatFileSize(imageBytes.size)}
                    Format: ${determineImageFormat(imageBytes)}
                    
                    Note: Image bytes were received but couldn't be sent directly due to API limitations.
                    Consider using image_url or image_cid instead.
                """.trimIndent(),
                parse_mode = com.github.omarmiatello.telegram.ParseMode.Markdown,
                reply_parameters = ReplyParameters(
                    message_id = MessageId(messageId),
                    chat_id = ChatId(groupId.toString())
                )
            )
            
            botApi.sendMessage(messageRequest)
            log.info("✅ Message sent with image information")
        }
    }

    private fun formatFileSize(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun determineImageFormat(imageBytes: ByteArray): String {
        if (imageBytes.size < 4) return "Unknown"
        
        return when {
            // JPEG
            imageBytes[0] == 0xFF.toByte() && imageBytes[1] == 0xD8.toByte() -> "JPEG"
            // PNG
            imageBytes[0] == 0x89.toByte() && imageBytes[1] == 0x50.toByte() && 
            imageBytes[2] == 0x4E.toByte() && imageBytes[3] == 0x47.toByte() -> "PNG"
            // GIF
            imageBytes[0] == 0x47.toByte() && imageBytes[1] == 0x49.toByte() && 
            imageBytes[2] == 0x46.toByte() -> "GIF"
            // WebP
            imageBytes[0] == 0x52.toByte() && imageBytes[1] == 0x49.toByte() && 
            imageBytes[2] == 0x46.toByte() && imageBytes[3] == 0x46.toByte() -> "WebP"
            else -> "Unknown"
        }
    }
}