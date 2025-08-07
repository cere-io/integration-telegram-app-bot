package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeImageEventPayload(
    @SerialName("user_id")
    val userId: Long,

    @SerialName("username")
    val userName: String,

    @SerialName("organization_id")
    val orgId: Int,

    @SerialName("campaign_id")
    val campaignId: String,

    @SerialName("group_id")
    val groupId: Long,

    @SerialName("message_id")
    val messageId: Long,

    @SerialName("image_url")
    val imageUrl: String,

    val prompt: String,

    @SerialName("prompt_tags")
    val promptTags: List<PromptTag> = emptyList(),
)