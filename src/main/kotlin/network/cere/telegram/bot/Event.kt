package network.cere.telegram.bot

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.*

data class Event(
    val payload: JsonNode,

    @JsonProperty("app_id")
    val appId: String,

    @JsonProperty("account_id")
    val accountId: String,

    val address: String,

    @JsonProperty("user_pub_key")
    val userPubKey: String,

    @JsonProperty("data_service_pub_key")
    val dataServicePubKey: String,

    val signing: String,

    @JsonProperty("event_type")
    val type: String,

    @JsonProperty("connection_id")
    val connectionId: String = UUID.randomUUID().toString(),

    @JsonProperty("session_id")
    val sessionId: String = UUID.randomUUID().toString(),

    val id: String = UUID.randomUUID().toString(),

    val timestamp: String = Instant.now().toString(),

    var signature: String? = null,
) {
    fun sign(signer: Signer): Event {
        val hash = "$id$type$timestamp".hash()
        val msg = "\u0019Ethereum Signed Message:\n${hash.length}${hash}".toByteArray()
        signature = signer.sign(msg).hex()
        return this
    }
}
