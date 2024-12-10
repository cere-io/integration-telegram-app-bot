package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.InlineKeyboardButton
import com.github.omarmiatello.telegram.InlineKeyboardMarkup

val replyKeyboardMarkup = InlineKeyboardMarkup(
    inline_keyboard = listOf(
        listOf(
            InlineKeyboardButton(
                text = "\uD83D\uDCC4 Connected channels",
                callback_data = "/connectedChannels"
            )
        ),
    )
)
