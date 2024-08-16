package network.cere.telegram.bot.streaming.ton

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckProofRequest(
    val address: String,
    val network: Int,
    @SerialName("public_key")
    val publicKey: String,
    val proof: Proof,
)

@Serializable
data class Proof(
    val timestamp: Long,
    val domain: Domain,
    val payload: String,
    val signature: String,
    @SerialName("state_init")
    val stateInit: String,
)

@Serializable
data class Domain(
    val lengthBytes: Int,
    val value: String,
)
