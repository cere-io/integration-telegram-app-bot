package network.cere.telegram.bot

import com.fasterxml.jackson.annotation.JsonProperty

data class MemeCallbackRequest(
    @JsonProperty("group_id")
    val groupId: Long,

    @JsonProperty("message_id")
    val messageId: Long,

    @JsonProperty("image_url")
    val imageUrl: String? = null,
    
    @JsonProperty("image_cid")
    val imageCid: String? = null,
) {
    init {
        require(imageUrl != null || imageCid != null) { 
            "Either imageUrl or imageCid must be provided" 
        }
    }
}
