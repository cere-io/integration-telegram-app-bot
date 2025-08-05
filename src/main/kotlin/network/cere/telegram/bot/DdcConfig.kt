package network.cere.telegram.bot

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "ddc")
interface DdcConfig {
    fun cdnUrl(): String
    fun bucket(): String
    fun mnemonic(): String
}