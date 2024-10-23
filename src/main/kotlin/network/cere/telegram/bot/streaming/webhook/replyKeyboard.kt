package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.InlineKeyboardButton
import com.github.omarmiatello.telegram.InlineKeyboardMarkup

val replyKeyboardMarkup = InlineKeyboardMarkup(
    inline_keyboard = listOf(
        listOf(
            InlineKeyboardButton(
                text = "\uD83D\uDCC4 Configure subscriptions",
                callback_data = "/configureSubscriptions"
            )
        ),
        listOf(
            InlineKeyboardButton(
                text = "\u2699\uFE0F Set Bot Access token",
                callback_data = "/setToken"
            ),
        ),
        listOf(
            InlineKeyboardButton(
                text = "\uD83D\uDCB3 Set payouts address",
                callback_data = "/setPayoutsAddress"
            ),
        ),
        listOf(
            InlineKeyboardButton(
                text = "\uD83C\uDF9E Add video",
                callback_data = "/addVideo"
            ),
        ),
        listOf(
            InlineKeyboardButton(
                text = "\uD83D\uDD0D Check configuration",
                callback_data = "/check"
            ),
        )
    )
)