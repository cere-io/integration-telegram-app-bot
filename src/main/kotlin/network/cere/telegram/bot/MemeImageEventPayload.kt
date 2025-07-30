package network.cere.telegram.bot

import com.fasterxml.jackson.annotation.JsonProperty

data class MemeImageEventPayload(
    @JsonProperty("organization_id")
    val orgId: Int,

    @JsonProperty("campaign_id")
    val campaignId: String,

    @JsonProperty("group_id")
    val groupId: Long,

    @JsonProperty("message_id")
    val messageId: Long,

    @JsonProperty("image_cid")
    val imageCid: String,

    val prompt: String,
)