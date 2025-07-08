package network.cere.telegram.bot

import kotlinx.serialization.Serializable

@Serializable
data class CereWalletResponse(
    val code: String,
    val data: CereWalletData,
)