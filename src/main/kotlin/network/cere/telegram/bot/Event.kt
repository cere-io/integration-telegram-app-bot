package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Event(
    val payload: EventPayload,

    @SerialName("app_id")
    val appId: String,

    @SerialName("account_id")
    val accountId: String,

    @SerialName("user_pub_key")
    val userPubKey: String,

    @SerialName("data_service_pub_key")
    val dataServicePubKey: String,

    val signing: String,

    @SerialName("event_type")
    val type: String,

    @SerialName("connection_id")
    val connectionId: String = UUID.randomUUID().toString(),

    @SerialName("session_id")
    val sessionId: String = UUID.randomUUID().toString(),

    val id: String = UUID.randomUUID().toString(),

    val timestamp: String = LocalDateTime.now().toString(),

    var signature: String? = null,
) {
    fun sign(signer: Signer): Event {
        val hash = "$id$type$timestamp".hash()
        val msg = "\u0019Ethereum Signed Message:\n${hash.length}${hash}".toByteArray()
        signature = signer.sign(msg).hex()
        return this
    }
}
