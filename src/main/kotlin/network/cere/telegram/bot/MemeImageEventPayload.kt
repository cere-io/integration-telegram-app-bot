package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SenderInfo(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String
)

@Serializable
data class MemeImageEventPayload(
    @SerialName("organization_id")
    val orgId: Int,

    @SerialName("campaign_id")
    val campaignId: String,

    @SerialName("group_id")
    val groupId: Long,

    @SerialName("message_id")
    val messageId: Long,

    @SerialName("image_cid")
    val imageCid: String,

    val prompt: String,

    @SerialName("sender_info")
    val senderInfo: SenderInfo
)