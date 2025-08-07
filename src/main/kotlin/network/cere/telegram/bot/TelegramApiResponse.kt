package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramApiResponse(
    @SerialName("ok")
    val ok: Boolean,
    
    @SerialName("result")
    val result: TelegramChat? = null,
    
    @SerialName("error_code")
    val errorCode: Int? = null,
    
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class TelegramChat(
    @SerialName("id")
    val id: Long,
    
    @SerialName("title")
    val title: String?,
    
    @SerialName("username")
    val channelName: String?,
    
    @SerialName("type")
    val type: String,
    
    @SerialName("active_usernames")
    val activeUsernames: List<String>? = null,
    
    @SerialName("has_visible_history")
    val hasVisibleHistory: Boolean? = null
) 