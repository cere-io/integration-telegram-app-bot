package network.cere.telegram.bot.streaming.webhook.command.impl.callback

import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.ddc.Wallet
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.webhook.BotProducer

@ApplicationScoped
class SetToken(
    private val botProducer: BotProducer,
    private val json: Json,
    private val wallet: Wallet,
) : AbstractBotCallbackCommand {
    override fun command() = "/setToken"

    //TODO check if admin
    override fun handle(update: Update) {
        val message = requireNotNull(update.callback_query?.message as Message)
        val from = requireNotNull(update.callback_query?.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)
        chatContext.entityName = "token"
        user.chatContextJson = json.encodeToString(chatContext)
        user.persistAndFlush()

        botProducer.sendTextMessage(
            message.chat.id,
            "Generate access token for public key from the next message and send it to me in base58 format"
        )
        botProducer.sendTextMessage(
            message.chat.id,
            wallet.publicKey,
        )
    }
}