package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoostEventPayload(
    @SerialName("organization_id")
    val orgId: Int,

    @SerialName("campaign_id")
    val campaignId: String,

    @SerialName("group_id")
    val groupId: Long,

    @SerialName("message_id")
    val messageId: Long,

    @SerialName("user_id")
    val userId: Long,

    @SerialName("username")
    val userName: String,
)