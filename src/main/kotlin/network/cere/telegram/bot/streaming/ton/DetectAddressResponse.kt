package network.cere.telegram.bot.streaming.ton

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetectAddressResponse(
    val result: Address,
)

@Serializable
data class Address(
    @SerialName("raw_form")
    val rawForm: String,
    val bounceable: B64Address,
    @SerialName("non_bounceable")
    val nonBounceable: B64Address,
)

@Serializable
data class B64Address(
    val b64: String,
    val b64url: String,
)