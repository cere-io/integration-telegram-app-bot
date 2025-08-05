package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageEventPayload(
    @SerialName("organization_id")
    val orgId: Int,

    @SerialName("campaign_id")
    val campaignId: String,

    @SerialName("group_id")
    val groupId: Long,

    @SerialName("message_id")
    val messageId: Long,

    @SerialName("date_unixtime")
    val dateUnixTime: Long,

    @SerialName("from_user_id")
    val fromUserId: Long,

    @SerialName("from_user_name")
    val fromUserName: String,

    val text: String,
)
