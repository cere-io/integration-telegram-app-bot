package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeCallbackRequest(
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
    val imageUrl: String? = null,

    @SerialName("image_cid")
    val imageCid: String? = null,

    @SerialName("image_base64")
    val imageBase64: String? = null, // Base64 encoded image data

    val prompt: String,
) {
    init {
        require(imageUrl != null || imageCid != null || imageBase64 != null) {
            "Either imageUrl, imageCid, or imageBase64 must be provided"
        }
    }
}
