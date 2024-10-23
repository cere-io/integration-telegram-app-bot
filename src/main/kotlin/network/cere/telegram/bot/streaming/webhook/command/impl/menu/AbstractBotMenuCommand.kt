package network.cere.telegram.bot.streaming.webhook.command.impl.menu

import network.cere.telegram.bot.streaming.webhook.command.BotCommand

interface AbstractBotMenuCommand : BotCommand {
    fun menuOrder(): Int

    fun description(): String
}