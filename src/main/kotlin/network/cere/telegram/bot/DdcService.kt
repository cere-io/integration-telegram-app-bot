package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import org.slf4j.LoggerFactory

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

@ApplicationScoped
class DdcService(
    private val telegramFileService: TelegramFileService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val json = Json { ignoreUnknownKeys = true }

    fun uri(cid: String) = "bucket/$cid"

    /**
     * Store binary file data in DDC (for images, documents, etc.)
     */
    fun storeFile(fileData: ByteArray, lastCid: String? = null): String {
        log.info("Storing file using File API")
        return telegramFileService.storeFile(fileData)
    }

    /**
     * Store text data (like messages) in DDC
     */
    fun store(event: String, lastCid: String?): String {
        return telegramFileService.storeFile(event.toByteArray())
    }

    /**
     * Get file from DDC by CID
     */
    fun getFile(cid: String): ByteArray {
        log.info("Getting file using File API")
        return telegramFileService.getFile(cid)
    }

    /**
     * Store a message in DDC
     */
    fun storeMessage(message: TelegramMessage, lastCid: String?): String {
        val messageJson = json.encodeToString(message)
        return store(messageJson, lastCid)
    }
}
