package network.cere.telegram.bot

import kotlinx.serialization.Serializable

@Serializable
data class CereWalletData(
    val accountId: String,
    val userPubKey: String,
)
