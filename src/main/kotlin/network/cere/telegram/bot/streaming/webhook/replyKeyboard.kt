package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.InlineKeyboardButton
import com.github.omarmiatello.telegram.InlineKeyboardMarkup

val replyKeyboardMarkup = InlineKeyboardMarkup(
    inline_keyboard = listOf(
        listOf(
            InlineKeyboardButton(
                text = "Set Bot Access Token",
                callback_data = "/setToken"
            ),
            InlineKeyboardButton(
                text = "Set payouts address",
                callback_data = "/setPayoutsAddress"
            ),
        ),
        listOf(
            InlineKeyboardButton(
                text = "Add video",
                callback_data = "/addVideo"
            ),
        ),
        listOf(
            InlineKeyboardButton(
                text = "Check configuration",
                callback_data = "/check"
            ),
        )
    )
)