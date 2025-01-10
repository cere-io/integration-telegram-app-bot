package network.cere.telegram.bot.streaming.webhook.command.impl.callback

import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.user.ContextEntity
import network.cere.telegram.bot.streaming.webhook.BotProducer

@ApplicationScoped
class SetGroup(
    private val botProducer: BotProducer,
    private val json: Json,
) : AbstractBotCallbackCommand {
    override fun command() = "/setGroup"

    override fun handle(update: Update) {
        val message = requireNotNull(update.callback_query?.message as Message)
        val from = requireNotNull(update.callback_query?.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)
        chatContext.entityName = ContextEntity.GROUP
        user.chatContextJson = json.encodeToString(chatContext)
        user.persistAndFlush()

        botProducer.sendTextMessage(
            message.chat.id,
            "Please send me the group link. Make sure:\n" +
                "1. The bot is added to the group as an admin\n" +
                "2. You are an admin of the group\n" +
                "3. The group has a public link",
        )
    }
}
