package network.cere.telegram.bot.streaming.webhook.command.impl.share

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import network.cere.telegram.bot.api.BotApi
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.channel.ChannelConfig
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.replyKeyboardMarkup
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class ShareChannel(
        @RestClient private val botApi: BotApi,
        private val botProducer: BotProducer,
        private val json: Json,
) : AbstractBotShareCommand {
    private val log = LoggerFactory.getLogger(javaClass)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun logChannelInfo(channelTitle: String, channelId: ChatId, admins: List<JsonElement>) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        log.info("\n==================== CHANNEL SHARED ====================")
        log.info("Timestamp: {}", timestamp)
        log.info("Channel Title: {}", channelTitle)
        log.info("Channel ID: {}", channelId.stringValue)
        log.info(
                "Total Members: {}",
                botApi.getChatMemberCount(TelegramRequest.GetChatMemberCountRequest(channelId))
                        .result
        )
        log.info("\nChannel Administrators:")
        admins.forEach { admin ->
            val adminObj = admin.jsonObject
            val userObj = adminObj["user"]?.jsonObject
            val status = adminObj["status"]?.jsonPrimitive?.content ?: "unknown"
            val name = userObj?.get("first_name")?.jsonPrimitive?.content ?: "unknown"
            val username = userObj?.get("username")?.jsonPrimitive?.content
            log.info(" - {} ({}) [{}]", name, username ?: "no username", status)
        }
        log.info("====================================================\n")
    }

    override fun requestId() = 100L

    override fun command() = "Share channel"

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
                            log.error("Failed to get chat administrators")
                            botProducer.sendTextMessage(
                                    currentChat,
                                    "Failed to get channel administrators"
                            )
                            return
                        }

                        val isAdmin =
                                adminsResult.asSequence().map { it.jsonObject }.any {
                                    it.getValue("user")
                                            .jsonObject
                                            .getValue("id")
                                            .jsonPrimitive
                                            .long == from.id.longValue &&
                                            it.getValue("status").jsonPrimitive.content in
                                                    setOf("administrator", "creator")
                                }

                        if (!isAdmin) {
                            log.warn("User {} is not an admin of the channel", from.id.longValue)
                            botProducer.sendTextMessage(
                                    currentChat,
                                    "You must be an administrator of the channel to share it"
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

                        user.chatContextJson =
                                json.encodeToString(ChatContext(channelId = sharedChatId.longValue))
                        user.persistAndFlush()

                        val memberCount =
                                botApi.getChatMemberCount(
                                                TelegramRequest.GetChatMemberCountRequest(
                                                        sharedChatId
                                                )
                                        )
                                        .result
                        val channelTitle =
                                sharedChat.result?.jsonObject?.getValue("title")
                                        ?.jsonPrimitive
                                        ?.content
                                        ?: run {
                                            log.error("Missing channel title")
                                            botProducer.sendTextMessage(
                                                    currentChat,
                                                    "Failed to get channel information"
                                            )
                                            return
                                        }

                        logChannelInfo(channelTitle, sharedChatId, adminsResult)

                        val channel =
                                Channel.findById(sharedChatId.longValue)
                                        ?: Channel(
                                                        id = sharedChatId.longValue,
                                                        config = ChannelConfig(),
                                                        username =
                                                                sharedChat.result?.jsonObject?.get(
                                                                                "username"
                                                                        )
                                                                        ?.jsonPrimitive
                                                                        ?.content,
                                                        title = channelTitle,
                                                        connectedAt = LocalDateTime.now(),
                                                        memberCount = memberCount?.toLong() ?: -1
                                                )
                                                .also { it.persistAndFlush() }

                        botProducer.sendTextMessage(
                                currentChat,
                                "Ok, let's configure channel ${channel.title}",
                                replyKeyboardMarkup
                        )
                    }
                    .onFailure {
                        log.error("Failed to get channel information", it)
                        botProducer.sendTextMessage(
                                currentChat,
                                "Failed to get channel information. Please make sure the bot is added to the channel."
                        )
                    }
        } catch (e: Exception) {
            log.error("Unexpected error while handling channel share", e)
            update.message?.chat?.id?.let { chatId ->
                botProducer.sendTextMessage(
                        chatId,
                        "An unexpected error occurred. Please try again."
                )
            }
        }
    }
}
