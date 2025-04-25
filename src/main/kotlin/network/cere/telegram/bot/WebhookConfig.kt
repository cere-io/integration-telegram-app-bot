package network.cere.telegram.bot

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "telegram.webhook")
interface WebhookConfig {
    fun url(): String
    fun token(): String
    fun maxConnections(): Int
}