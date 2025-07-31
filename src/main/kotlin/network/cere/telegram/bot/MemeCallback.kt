package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.MessageId
import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
@Path("meme")
class MemeCallback(
    @RestClient private val botApi: BotApi,
    private val ddcService: DdcService,
    private val botConfig: BotConfig,
    @ConfigProperty(name = "ddc.bucket") private val bucket: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    @POST
    fun replyWithMeme(rq: MemeCallbackRequest) {
        log.info("Processing meme callback: groupId={}, messageId={}, imageUrl={}, imageCid={}", 
                 rq.groupId, rq.messageId, rq.imageUrl, rq.imageCid)
        
        val chatId = ChatId(rq.groupId.toString())
        
        try {
            val imageUrl = when {
                rq.imageUrl != null -> {
                    log.info("Using provided image URL: {}", rq.imageUrl)
                    rq.imageUrl
                }
                rq.imageCid != null -> {
                    val ddcUrl = "https://cdn.testnet.cere.network/$bucket/${rq.imageCid}/"
                    log.info("Converting CID to DDC URL: {}", ddcUrl)
                    ddcUrl
                }
                else -> throw IllegalArgumentException("Neither imageUrl nor imageCid provided")
            }
            
            try {
                val photoRequest = TelegramRequest.SendPhotoRequest(
                    chat_id = chatId,
                    photo = imageUrl,
                    caption = "✅ Here is your processed meme!",
                    reply_parameters = ReplyParameters(
                        message_id = MessageId(rq.messageId),
                        chat_id = chatId
                    )
                )
                botApi.sendPhoto(photoRequest)
                log.info("✅ Meme sent successfully using URL")
                
            } catch (urlException: Exception) {
                log.warn("Failed to send using URL, trying with multipart upload: {}", urlException.message)
                
                if (rq.imageCid != null) {
                    sendImageFromDdc(chatId, rq.messageId, rq.imageCid)
                } else {
                    throw urlException
                }
            }
            
        } catch (e: Exception) {
            log.error("Failed to send meme", e)
            
            val fallbackRequest = TelegramRequest.SendMessageRequest(
                chat_id = chatId,
                text = "✅ Your meme is ready!\n🔗 ${rq.imageCid ?: rq.imageUrl}\n(Could not display image: ${e.message})",
                reply_parameters = ReplyParameters(
                    message_id = MessageId(rq.messageId),
                    chat_id = chatId
                )
            )
            botApi.sendMessage(fallbackRequest)
        }
    }
    
    private fun sendImageFromDdc(chatId: ChatId, messageId: Long, cid: String) {
        log.info("Downloading image from DDC for multipart upload: {}", cid)
        
        val downloadedBytes = ddcService.getFile(cid)
        
        val (extension, mimeType) = when {
            downloadedBytes.size >= 3 && downloadedBytes[0] == 0xFF.toByte() && 
            downloadedBytes[1] == 0xD8.toByte() && downloadedBytes[2] == 0xFF.toByte() -> "jpg" to "image/jpeg"
            downloadedBytes.size >= 8 && downloadedBytes.sliceArray(1..3).contentEquals("PNG".toByteArray()) -> "png" to "image/png"
            downloadedBytes.size >= 6 && downloadedBytes.sliceArray(0..5).contentEquals("GIF87a".toByteArray()) -> "gif" to "image/gif"
            downloadedBytes.size >= 6 && downloadedBytes.sliceArray(0..5).contentEquals("GIF89a".toByteArray()) -> "gif" to "image/gif"
            else -> "jpg" to "image/jpeg"
        }
        
        val httpClient = java.net.http.HttpClient.newHttpClient()
        val boundary = "----FormBoundary" + System.currentTimeMillis()
        
        val multipartData = buildString {
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
            append("${chatId.longValue}\r\n")
            
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
            append("✅ Here is your processed meme!\r\n")
            
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"reply_to_message_id\"\r\n\r\n")
            append("$messageId\r\n")
            
            append("--$boundary\r\n")
            append("Content-Disposition: form-data; name=\"photo\"; filename=\"meme.$extension\"\r\n")
            append("Content-Type: $mimeType\r\n\r\n")
        }
        
        val multipartBytes = multipartData.toByteArray() + downloadedBytes + "\r\n--$boundary--\r\n".toByteArray()
        
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("https://api.telegram.org/bot${botConfig.token()}/sendPhoto"))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(multipartBytes))
            .build()
        
        val response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() == 200) {
            log.info("✅ Meme sent successfully using multipart upload")
        } else {
            log.error("❌ Failed to send meme via multipart: {} - {}", response.statusCode(), response.body())
            throw RuntimeException("Telegram API error: ${response.body()}")
        }
    }
}
