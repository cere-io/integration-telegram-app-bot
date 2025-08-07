package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramChannelSearchResponse(
    @SerialName("success")
    val success: Boolean,
    
    @SerialName("channels")
    val channels: List<TelegramChannel> = emptyList(),
    
    @SerialName("error")
    val error: String? = null
)

@Serializable
data class TelegramChannel(
    @SerialName("id")
    val id: Long,
    
    @SerialName("title")
    val title: String,
    
    @SerialName("name")
    val name: String?,
    
    @SerialName("type")
    val type: String,
    
    @SerialName("member_count")
    val memberCount: Int? = null
) 