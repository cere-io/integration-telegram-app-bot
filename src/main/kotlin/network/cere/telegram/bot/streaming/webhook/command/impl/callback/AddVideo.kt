package network.cere.telegram.bot.streaming.webhook.command.impl.callback

import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.user.ContextEntity
import network.cere.telegram.bot.streaming.user.ContextModificationStep
import network.cere.telegram.bot.streaming.webhook.BotProducer

@ApplicationScoped
class AddVideo(
    private val botProducer: BotProducer,
    private val json: Json,
) : AbstractBotCallbackCommand {
    override fun command() = "/addVideo"

    override fun handle(update: Update) {
        val message = requireNotNull(update.callback_query?.message as Message)
        val from = requireNotNull(update.callback_query?.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)
        chatContext.entityName = ContextEntity.VIDEO
        chatContext.modificationStep = ContextModificationStep.URL
        user.chatContextJson = json.encodeToString(chatContext)
        user.persistAndFlush()

        botProducer.sendTextMessage(
            message.chat.id,
            "Send me the video URL"
        )
    }
}