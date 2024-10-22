package network.cere.telegram.bot.streaming.webhook.command.impl.callback

import com.github.omarmiatello.telegram.InlineKeyboardButton
import com.github.omarmiatello.telegram.InlineKeyboardMarkup
import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.replyKeyboardMarkup
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class Check(
    @ConfigProperty(name = "telegram.bot.username") private val botUsername: String,
    private val botProducer: BotProducer,
    private val json: Json,
) : AbstractBotCallbackCommand {
    override fun command() = "/check"

    override fun handle(update: Update) {
        val from = requireNotNull(update.callback_query?.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        val currentChannel = requireNotNull(json.decodeFromString<ChatContext>(user.chatContextJson).channelId)
        val channel = requireNotNull(Channel.findById(currentChannel))
        val channelConfig = channel.config
        val reply = StringBuilder("Config for channel ${channel.title}:\n")
        if (channelConfig.botDdcAccessTokenBase58 != null) {
            reply.append("Bot access token is configured\n")
        } else {
            reply.append("Bot access token is not configured\n")
        }
        if (channelConfig.payoutAddress != null) {
            reply.append("Payouts address is configured\n")
        } else {
            reply.append("Payouts address is not configured\n")
        }
        reply.append("Number of videos: ${channel.videos.size}\n")
        if (channel.isConfigured()) {
            reply.append("You are all set. Share this link in your channel:\nhttps://t.me/$botUsername/${channel.config.connectedApp}?startapp=${channel.id}")
        }
        botProducer.sendTextMessage(
            requireNotNull(update.callback_query?.message as Message).chat.id,
            reply.toString(),
            replyKeyboardMarkup
        )
    }
}