package network.cere.telegram.bot

import com.fasterxml.jackson.annotation.JsonProperty

data class MessageEventPayload(
    @JsonProperty("organization_id")
    val orgId: Int,

    @JsonProperty("campaign_id")
    val campaignId: String,

    @JsonProperty("group_id")
    val groupId: Long,

    @JsonProperty("message_id")
    val messageId: Long,

    @JsonProperty("date_unixtime")
    val dateUnixTime: Long,

    @JsonProperty("from_user_id")
    val fromUserId: Long,

    @JsonProperty("from_user_name")
    val fromUserName: String,

    val text: String,
)
