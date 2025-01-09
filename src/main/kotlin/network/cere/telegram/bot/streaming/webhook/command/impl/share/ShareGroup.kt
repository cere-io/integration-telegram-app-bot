package network.cere.telegram.bot.streaming.webhook.command.impl.share

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

                        val adminsResult =
                                runCatching {
                                            botApi.getChatAdministrators(
                                                            TelegramRequest
                                                                    .GetChatAdministratorsRequest(
                                                                            sharedChatId
                                                                    )
                                                    )
                                                    .result
                                        }
                                        .getOrNull()

                        if (adminsResult == null) {
                            log.error("Failed to get group administrators")
                            botProducer.sendTextMessage(
                                    currentChat,
                                    "Failed to get group administrators"
                            )
                            return
                        }

                        val isAdmin =
                                adminsResult.asSequence().map { it.jsonObject }.any {
                                    it["user"]
                                            ?.jsonObject
                                            ?.get("id")
                                            ?.jsonPrimitive
                                            ?.content
                                            ?.toLong() == from.id.longValue &&
                                            it["status"]?.jsonPrimitive?.content in
                                                    setOf("administrator", "creator")
                                }

                        if (!isAdmin) {
                            log.warn("User {} is not an admin of the group", from.id.longValue)
                            botProducer.sendTextMessage(
                                    currentChat,
                                    "You must be an administrator of the group to share it"
                            )
                            return
                        }

                        val user =
                                BotUser.findById(from.id.longValue)
                                        ?: run {
                                            log.error("User not found: {}", from.id.longValue)
                                            botProducer.sendTextMessage(
                                                    currentChat,
                                                    "Failed to process your request. Please try again."
                                            )
                                            return
                                        }

                        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)
                        val currentChannel =
                                chatContext.channelId
                                        ?: run {
                                            log.error(
                                                    "No channel selected for user {}",
                                                    from.id.longValue
                                            )
                                            botProducer.sendTextMessage(
                                                    currentChat,
                                                    "Please share a channel first."
                                            )
                                            return
                                        }

                        val channel =
                                Channel.findById(currentChannel)
                                        ?: run {
                                            log.error("Channel not found: {}", currentChannel)
                                            botProducer.sendTextMessage(
                                                    currentChat,
                                                    "Failed to find the channel. Please try again."
                                            )
                                            return
                                        }

                        channel.config.connectedGroupId = sharedChatId.longValue
                        channel.persistAndFlush()

                        val groupTitle =
                                sharedChat.result?.jsonObject?.get("title")?.jsonPrimitive?.content
                                        ?: "Unknown Group"
                        botProducer.sendTextMessage(
                                currentChat,
                                "Successfully connected group $groupTitle to channel ${channel.title}",
                                replyKeyboardMarkup
                        )
                    }
                    .onFailure {
                        log.error("Failed to get group information", it)
                        botProducer.sendTextMessage(
                                currentChat,
                                "Failed to get group information. Please make sure the bot is added to the group."
                        )
                    }
        } catch (e: Exception) {
            log.error("Unexpected error while handling group share", e)
            update.message?.chat?.id?.let { chatId ->
                botProducer.sendTextMessage(
                        chatId,
                        "An unexpected error occurred. Please try again."
                )
            }
        }
    }
}
