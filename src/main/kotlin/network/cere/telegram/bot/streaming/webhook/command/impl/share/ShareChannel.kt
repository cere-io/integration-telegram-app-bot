package network.cere.telegram.bot.streaming.webhook.command.impl.share

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import network.cere.telegram.bot.api.BotApi
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.channel.ChannelConfig
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.replyKeyboardMarkup
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
class ShareChannel(
    @RestClient private val botApi: BotApi,
    private val botProducer: BotProducer,
    private val json: Json,
) : AbstractBotShareCommand {
    override fun requestId() = 100L

    override fun command() = "Share channel"

    override fun handle(update: Update) {
        val currentChat = requireNotNull(update.message?.chat?.id)
        val sharedChatId = ChatId(requireNotNull(update.message?.chat_shared).chat_id.stringValue)
        runCatching { botApi.getChat(TelegramRequest.GetChatRequest(sharedChatId)) }
            .onSuccess { sharedChat ->
                val from = requireNotNull(update.message?.from)
                val isAdmin = requireNotNull(
                    botApi.getChatAdministrators(
                        TelegramRequest.GetChatAdministratorsRequest(sharedChatId)
                    ).result
                )
                    .asSequence()
                    .map { it.jsonObject }
                    .any {
                        it.getValue("user").jsonObject.getValue("id").jsonPrimitive.long == from.id.longValue
                                && it.getValue("status").jsonPrimitive.content in setOf("administrator", "creator")
                    }
                if (!isAdmin) return
                val user = requireNotNull(BotUser.findById(from.id.longValue))
                user.chatContextJson = json.encodeToString(ChatContext(channelId = sharedChatId.longValue))
                user.persistAndFlush()
                val channel = Channel.findById(sharedChatId.longValue) ?: Channel(
                    id = sharedChatId.longValue,
                    config = ChannelConfig(),
                    title = requireNotNull(sharedChat.result?.jsonObject?.getValue("title").toString())
                ).also {
                    it.persistAndFlush()
                }
                botProducer.sendTextMessage(
                    currentChat,
                    "Ok, let's configure channel ${channel.title}",
                    replyKeyboardMarkup
                )
            }
            .onFailure {
                botProducer.sendTextMessage(currentChat, "Add bot to the channel first")
            }
    }
}