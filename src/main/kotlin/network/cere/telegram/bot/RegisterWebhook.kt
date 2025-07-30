package network.cere.telegram.bot

import io.quarkus.runtime.Startup
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import jakarta.json.Json
import jakarta.json.JsonObject

@Startup
class RegisterWebhook(webhookConfig: WebhookConfig, botConfig: BotConfig) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.info("Registering Telegram webhook")
        try {
            setWebhookManually(webhookConfig, botConfig)
            log.info("Telegram webhook set successfully")
        } catch (e: Exception) {
            log.warn("Failed to set webhook automatically: ${e.message}. You may need to set it manually.")
        }
    }

    private fun setWebhookManually(webhookConfig: WebhookConfig, botConfig: BotConfig) {
        val webhookPayload = Json.createObjectBuilder()
            .add("url", webhookConfig.url())
            .add("secret_token", webhookConfig.token())
            .add("max_connections", webhookConfig.maxConnections())
            .add("drop_pending_updates", true)
            .add("allowed_updates", Json.createArrayBuilder()
                .add("message")
                .add("callback_query")
                .add("chat_member")
                .build())
            .build()

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.telegram.org/bot${botConfig.token()}/setWebhook"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(webhookPayload.toString()))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to set webhook: ${response.body()}")
        }
        
        log.debug("Webhook response: ${response.body()}")
    }
}
