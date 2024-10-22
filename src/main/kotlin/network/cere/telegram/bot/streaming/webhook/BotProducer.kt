package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.*
import jakarta.enterprise.context.ApplicationScoped
import network.cere.telegram.bot.api.BotApi
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
class BotProducer(@RestClient private val botApi: BotApi) {
    fun sendTextMessage(
        chatId: ChatId,
        text: String,
        replyMarkup: KeyboardOption? = null,
    ) {
        val telegramRequest = TelegramRequest.SendMessageRequest(
            chat_id = chatId,
            text = text,
            reply_markup = replyMarkup,
        )
        botApi.sendMessage(telegramRequest)
    }
}