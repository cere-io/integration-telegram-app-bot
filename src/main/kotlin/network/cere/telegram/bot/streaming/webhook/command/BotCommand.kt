package network.cere.telegram.bot.streaming.webhook.command

import com.github.omarmiatello.telegram.Update

interface BotCommand {
    fun handle(update: Update)
}