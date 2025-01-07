package network.cere.telegram.bot.streaming.webhook.command

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import network.cere.telegram.bot.streaming.webhook.command.impl.channel.ChannelMessageHandler
import network.cere.telegram.bot.streaming.webhook.command.impl.chat.ChatMemberCommand
import network.cere.telegram.bot.streaming.webhook.command.impl.share.AbstractBotShareCommand
import network.cere.telegram.bot.streaming.webhook.command.impl.text.BotTextCommand
import org.slf4j.LoggerFactory

@ApplicationScoped
class BotCommands(
        commands: Instance<BotCommand>,
        private val botTextCommand: BotTextCommand,
        private val chatMemberCommand: ChatMemberCommand,
        private val channelMessageHandler: ChannelMessageHandler
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val commandsMap = commands.associateBy(BotCommand::command)
    private val shareCommands =
            commands.asSequence()
                    .filter { it is AbstractBotShareCommand }
                    .map { it as AbstractBotShareCommand }
                    .associateBy(AbstractBotShareCommand::requestId)

    fun tryHandle(update: Update) {
        try {
            log.debug("Processing update: {}", update.toJson())

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
                        commandsMap[requireNotNull(update.callback_query).data]
                    }
                    update.message?.chat_shared != null -> {
                        log.debug(
                                "Processing chat share with request_id: {}",
                                update.message?.chat_shared?.request_id
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
