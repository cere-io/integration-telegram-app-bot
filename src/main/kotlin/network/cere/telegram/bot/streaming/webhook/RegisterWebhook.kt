package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.TelegramRequest
import io.quarkus.runtime.Startup
import network.cere.telegram.bot.api.BotApi
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@Startup
class RegisterWebhook(@RestClient botApi: BotApi, webhookConfig: WebhookConfig) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("Registering Telegram webhook")
        val rq = TelegramRequest.SetWebhookRequest(
            url = webhookConfig.url(),
            max_connections = webhookConfig.maxConnections().toLong(),
            drop_pending_updates = true,
            secret_token = webhookConfig.token(),
            allowed_updates = listOf("message", "callback_query", "chat_member")
        )
        botApi.setWebhook(rq)
        log.info("Telegram webhook set")
    }
}
