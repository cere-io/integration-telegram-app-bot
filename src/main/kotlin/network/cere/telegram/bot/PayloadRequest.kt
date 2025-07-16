package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PayloadRequest(
    val payload: JsonElement,
    val source: String? = null,
    val metadata: Map<String, String>? = null,
    @SerialName("target_chats")
    val targetChats: List<String>? = null,
    val priority: PayloadPriority = PayloadPriority.NORMAL,
)

@Serializable
enum class PayloadPriority {
    LOW, NORMAL, HIGH, URGENT
} 