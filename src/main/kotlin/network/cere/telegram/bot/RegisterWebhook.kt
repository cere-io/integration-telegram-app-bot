package network.cere.telegram.bot

import com.github.omarmiatello.telegram.BotCommand
import com.github.omarmiatello.telegram.TelegramRequest
import io.quarkus.runtime.Startup
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

        val commands = listOf(
            BotCommand("generate", "Apply a filter to the attached image"),
            BotCommand("filters", "Lists all available filters"),
            BotCommand("help", "Lists of all commands")
        )
        runCatching {
            botApi.setMyCommands(TelegramRequest.SetMyCommandsRequest(commands))
            log.info("Bot commands set successfully")
        }.onFailure { log.error("Failed to set bot commands", it) }
    }
}
