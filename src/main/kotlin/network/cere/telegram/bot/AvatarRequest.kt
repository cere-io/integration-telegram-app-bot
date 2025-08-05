package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AvatarRequestWrapper(
    val params: AvatarParams
)

@Serializable
data class AvatarParams(
    @SerialName("user_id")
    val userId: String,

    @SerialName("organization_id")
    val orgId: String,

    @SerialName("campaign_id")
    val campaignId: String
) 