package network.cere.telegram.bot.streaming.ton

import kotlinx.serialization.Serializable

@Serializable
data class GetAddressBalanceResponse(
    val result: String,
)