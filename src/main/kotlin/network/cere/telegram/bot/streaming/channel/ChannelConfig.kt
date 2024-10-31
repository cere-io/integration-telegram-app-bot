package network.cere.telegram.bot.streaming.channel

import jakarta.persistence.Embeddable

@Embeddable
data class ChannelConfig(
    var botDdcAccessTokenBase58: String? = null,
    var payoutAddress: String? = null,
    var connectedApp: String = "cere", //TODO configure from bot apps
)
