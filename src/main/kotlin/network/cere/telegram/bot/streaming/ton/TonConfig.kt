package network.cere.telegram.bot.streaming.ton

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "ton")
interface TonConfig {
    fun api(): Api
    fun wallet(): Wallet

    interface Api {
        fun url(): String
        fun token(): String
    }

    interface Wallet {
        fun bounceable(): String
        fun nonbounceable(): String
    }
}