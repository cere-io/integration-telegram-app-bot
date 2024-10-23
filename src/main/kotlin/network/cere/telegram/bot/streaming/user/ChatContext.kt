package network.cere.telegram.bot.streaming.user

import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import kotlinx.serialization.Serializable

@Serializable
data class ChatContext(
    val channelId: Long? = null,

    @Enumerated(EnumType.STRING)
    var entityName: ContextEntity? = null,

    var entityId: Long? = null,

    @Enumerated(EnumType.STRING)
    var modificationStep: ContextModificationStep? = null,
)
