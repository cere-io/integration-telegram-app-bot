package network.cere.telegram.bot.streaming.ddc

import io.smallrye.config.ConfigMapping
import network.cere.ddc.Signature

@ConfigMapping(prefix = "ddc")
interface DdcConfig {
    fun wallet(): Wallet

    interface Wallet {
        fun mnemonic(): String
        fun algorithm(): Signature.Algorithm
    }

    fun token(): Token

    interface Token {
        fun duration(): Long
    }
}