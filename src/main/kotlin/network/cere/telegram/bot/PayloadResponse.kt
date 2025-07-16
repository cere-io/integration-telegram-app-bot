package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PayloadResponse(
    val success: Boolean,
    val message: String,
    @SerialName("processedType")
    val processedType: String,
    @SerialName("sentToChatCount")
    val sentToChatCount: Int,
    val errors: List<String> = emptyList(),
    val timestamp: String = Instant.now().toString(),
) 