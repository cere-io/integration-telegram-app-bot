package network.cere.telegram.bot.streaming.ton

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "ton")
interface TonConfig {
    fun api(): Api

    interface Api {
        fun url(): String
        fun token(): String
    }
}