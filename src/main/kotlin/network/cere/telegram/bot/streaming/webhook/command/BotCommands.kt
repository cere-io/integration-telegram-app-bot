package network.cere.telegram.bot.streaming.webhook.command

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import network.cere.telegram.bot.streaming.webhook.command.impl.share.AbstractBotShareCommand
import network.cere.telegram.bot.streaming.webhook.command.impl.text.BotTextCommand

@ApplicationScoped
class BotCommands(commands: Instance<BotCommand>, private val botTextCommand: BotTextCommand) {
    private val commandsMap = commands.associateBy(BotCommand::command)
    private val shareCommands = commands.asSequence()
        .filter { it is AbstractBotShareCommand }
        .map { it as AbstractBotShareCommand }
        .associateBy(AbstractBotShareCommand::requestId)

    fun tryHandle(update: Update) {
        val command = when {
            update.message?.text != null -> commandsMap[requireNotNull(update.message).text]
            update.callback_query?.data != null -> commandsMap[requireNotNull(update.callback_query).data]
            update.message?.chat_shared != null -> shareCommands[requireNotNull(update.message?.chat_shared).request_id]
            else -> null
        }
        if (command != null) command.handle(update) else botTextCommand.tryHandle(update)
    }
}