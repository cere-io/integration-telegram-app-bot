package network.cere.telegram.bot.streaming.ton

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetTransactionsResponse(
    val result: List<Transaction>,
)

@Serializable
data class Transaction(
    @SerialName("in_msg")
    val inMsg: InMsg,
)

@Serializable
data class InMsg(
    val source: String,
    val destination: String,
    val value: Long,
    val message: String,
)
