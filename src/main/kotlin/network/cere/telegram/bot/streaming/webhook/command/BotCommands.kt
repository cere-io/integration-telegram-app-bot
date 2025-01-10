package network.cere.telegram.bot.streaming.webhook.command

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.ddc.Wallet
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.AddVideo
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.Check
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.ConfigureSubscriptions
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.SetGroup
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.SetPayoutsAddress
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.SetToken
import network.cere.telegram.bot.streaming.webhook.command.impl.channel.ChannelMessageHandler
import network.cere.telegram.bot.streaming.webhook.command.impl.chat.ChatMemberCommand
import network.cere.telegram.bot.streaming.webhook.command.impl.group.GroupMessageHandler
import network.cere.telegram.bot.streaming.webhook.command.impl.menu.Start
import network.cere.telegram.bot.streaming.webhook.command.impl.share.ShareChannel
import network.cere.telegram.bot.streaming.webhook.command.impl.text.BotTextCommand
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

@ApplicationScoped
class BotCommands(
    private val botTextCommand: BotTextCommand,
    private val channelMessageHandler: ChannelMessageHandler,
    private val groupMessageHandler: GroupMessageHandler,
    private val chatMemberCommand: ChatMemberCommand,
    private val shareChannel: ShareChannel,
    private val botProducer: BotProducer,
    private val json: Json,
    @ConfigProperty(name = "TELEGRAM_BOT_USERNAME") private val botUsername: String,
    private val wallet: Wallet,
    private val start: Start,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val commandsMap =
        mapOf(
            "/start" to start,
            "Configure subscriptions" to ConfigureSubscriptions(botProducer, json),
            "Set Bot Access token" to SetToken(botProducer, json, wallet),
            "Set payouts address" to SetPayoutsAddress(botProducer, json),
            "Set group" to SetGroup(botProducer, json),
            "Add video" to AddVideo(botProducer, json),
            "Check configuration" to Check(botUsername, botProducer, json),
        )
    private val shareCommands =
        mapOf(
            shareChannel.requestId() to shareChannel,
        )

    fun handle(update: Update) {
        try {
            // Check message type and handle accordingly
            when (update.message?.chat?.type) {
                "channel" -> {
                    log.debug("Handling channel message")
                    channelMessageHandler.handle(update)
                    return
                }
                "group", "supergroup" -> {
                    val message = update.message
                    val chat = message?.chat
                    log.info("=== GROUP MESSAGE DEBUG ===")
                    log.info("Received message from group:")
                    log.info("Group ID: {}", chat?.id)
                    log.info("Group Type: {}", chat?.type)
                    log.info("Group Title: {}", chat?.title)
                    log.info("Message Text: {}", message?.text)
                    log.info("From User: {} (ID: {})", message?.from?.first_name, message?.from?.id)
                    log.info("Is Bot Member: {}", message?.from?.is_bot)
                    log.info("=========================")

                    // For privacy mode enabled bots, we only process:
                    // 1. Commands explicitly meant for this bot
                    // 2. Replies to bot's messages
                    // 3. Messages sent via this bot
                    if (message?.text?.startsWith("/") == true ||
                        message?.reply_to_message?.from?.is_bot == true
                    ) {
                        log.info("Processing command or reply to bot message")
                        handleCommand(update)
                    } else {
                        log.info("Message doesn't match processing criteria - privacy mode active")
                        groupMessageHandler.handle(update)
                    }
                    return
                }
            }

            handleCommand(update)
        } catch (e: Exception) {
            log.error("Failed to process update: {}", update.toJson().replace("\n", ""), e)
        }
    }

    private fun handleCommand(update: Update) {
        val command =
            when {
                update.message?.text != null -> {
                    log.debug("Processing text message: {}", update.message?.text)
                    commandsMap[requireNotNull(update.message).text]
                }
                update.callback_query?.data != null -> {
                    log.debug("Processing callback query: {}", update.callback_query?.data)
                    val data = requireNotNull(update.callback_query).data
                    commandsMap.entries.find { data == it.value.command() }?.value
                }
                update.message?.chat_shared != null -> {
                    log.debug(
                        "Processing chat share with request_id: {}",
                        update.message?.chat_shared?.request_id,
                    )
                    shareCommands[requireNotNull(update.message?.chat_shared).request_id]
                }
                update.chat_member != null -> {
                    log.debug("Processing chat member update")
                    chatMemberCommand
                }
                else -> {
                    log.debug("No matching command type found")
                    null
                }
            }

        if (command != null) {
            log.debug("Executing command: {}", command.javaClass.simpleName)
            command.handle(update)
        } else {
            log.debug("Falling back to text command handler")
            botTextCommand.tryHandle(update)
        }
    }
}
