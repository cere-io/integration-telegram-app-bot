package network.cere.telegram.bot

import com.iwebpp.crypto.TweetNaclFast
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Serializable
data class TelegramMessage(
    val userId: String,
    val chatId: String,
    val messageId: String,
    val text: String?,
    val timestamp: String,
    val messageType: String,
    val botResponse: String? = null
)

@Serializable
data class DdcStoreRequest(
    val data: String, // Base64 encoded data
    val bucket: String,
    val mnemonic: String
)

@Serializable
data class DdcStoreResponse(
    val cid: String,
    val success: Boolean,
    val error: String? = null
)

// Old REST API interface removed - now using gRPC via TelegramDdcService

@ApplicationScoped
class DdcService(
    private val telegramDdcService: TelegramDdcService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    fun uri(cid: String) = "bucket/$cid"

    /**
     * Store binary file data in DDC (for images, documents, etc.)
     */
    fun storeFile(fileData: ByteArray, lastCid: String? = null): String {
        log.info("Delegating to TelegramDdcService for gRPC implementation")
        return telegramDdcService.storeFile(fileData, lastCid)
    }

    /**
     * Store text data (like messages) in DDC
     */
    fun store(event: String, lastCid: String?): String {
        return telegramDdcService.storeFile(event.toByteArray(), lastCid)
    }

    /**
     * Store a message in DDC
     */
    fun storeMessage(message: TelegramMessage, lastCid: String?): String {
        val messageJson = json.encodeToString(message)
        return store(messageJson, lastCid)
    }

    /**
     * Store a batch of messages for better efficiency
     */
    fun storeBatch(appId: String, messages: List<TelegramMessage>): String {
        log.info("Storing batch of ${messages.size} messages for app $appId in DDC")
        
        // Create batch payload
        val batchPayload = mapOf(
            "batch_id" to generateBatchId(appId),
            "app_id" to appId,
            "timestamp" to Instant.now().toString(),
            "event_count" to messages.size,
            "messages" to messages,
            "batch_type" to "telegram_message_batch"
        )
        
        // Store the batch as a single DDC entry
        val batchCid = store(json.encodeToString(batchPayload), null)
        
        log.info("Batch stored in DDC with CID: $batchCid")
        return batchCid
    }
    
    private fun generateBatchId(appId: String): String {
        return "batch_${appId}_${System.currentTimeMillis()}"
    }
}