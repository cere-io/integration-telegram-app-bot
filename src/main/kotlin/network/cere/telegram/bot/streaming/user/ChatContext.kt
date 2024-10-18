package network.cere.telegram.bot.streaming.user

import kotlinx.serialization.Serializable

@Serializable
data class ChatContext(
    val channelId: Long? = null,
    var entityName: String? = null,
    var entityId: Long? = null,
    var modificationStep: String? = null,
)
