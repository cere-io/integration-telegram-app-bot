package network.cere.telegram.bot.streaming.webhook.command.impl.share

import com.github.omarmiatello.telegram.*
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.api.BotApi
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.replyKeyboardMarkup
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class ShareGroup(
        @RestClient private val botApi: BotApi,
        private val botProducer: BotProducer,
        private val json: Json,
) : AbstractBotShareCommand {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun requestId() = 101L

    override fun command() = "Share group"

    override fun handle(update: Update) {
        try {
            val currentChat =
                    update.message?.chat?.id
                            ?: run {
                                log.error("Missing chat ID in update")
                                return
                            }
            val chatShared =
                    update.message?.chat_shared
                            ?: run {
                                log.error("Missing chat_shared in update")
                                return
                            }
            val sharedChatId = ChatId(chatShared.chat_id.stringValue)

            runCatching { botApi.getChat(TelegramRequest.GetChatRequest(sharedChatId)) }
                    .onSuccess { sharedChat ->
                        val from =
                                update.message?.from
                                        ?: run {
                                            log.error("Missing from field in message")
                                            return
                                        }
                        val user = requireNotNull(BotUser.findById(from.id.longValue))
                        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)
                        val currentChannel = requireNotNull(chatContext.channelId)
                        val channel = requireNotNull(Channel.findById(currentChannel))

                        // Store the group ID
                        channel.config.connectedGroupId = chatShared.chat_id.longValue
                        channel.persistAndFlush()

                        // Clear context and show success message
                        chatContext.entityName = null
                        user.chatContextJson = json.encodeToString(chatContext)
                        user.persistAndFlush()

                        botProducer.sendTextMessage(
                                currentChat,
                                "Successfully connected group ${sharedChat.result.toString()} to channel ${channel.title}",
                                replyKeyboardMarkup
                        )
                    }
                    .onFailure {
                        log.error("Failed to get chat info", it)
                        botProducer.sendTextMessage(
                                currentChat,
                                "Failed to connect to the group. Make sure the bot is a member of the group and has admin rights.",
                                replyKeyboardMarkup
                        )
                    }
        } catch (e: Exception) {
            log.error("Error processing shared group", e)
        }
    }
}
