package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemeCallbackRequest(
    @SerialName("group_id")
    val groupId: Long,

    @SerialName("message_id")
    val messageId: Long,

    @SerialName("image_url")
    val imageUrl: String? = null,
    
    @SerialName("image_cid")
    val imageCid: String? = null,
) {
    init {
        require(imageUrl != null || imageCid != null) { 
            "Either imageUrl or imageCid must be provided" 
        }
    }
}
