package network.cere.telegram.bot.streaming.webhook

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "telegram.webhook")
interface WebhookConfig {
    fun url(): String
    fun token(): String
    fun maxConnections(): Int
}