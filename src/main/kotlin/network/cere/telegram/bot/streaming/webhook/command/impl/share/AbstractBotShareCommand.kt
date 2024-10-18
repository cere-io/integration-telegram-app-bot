package network.cere.telegram.bot.streaming.webhook.command.impl.share

import network.cere.telegram.bot.streaming.webhook.command.BotCommand

interface AbstractBotShareCommand : BotCommand {
    fun requestId(): Long
}