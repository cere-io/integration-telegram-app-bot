package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramChannelSearchRequest(
    @SerialName("channelName")
    val channelName: String
) 