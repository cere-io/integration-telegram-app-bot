package network.cere.telegram.bot

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "telegram.bot")
interface BotConfig {
    fun token(): String
}