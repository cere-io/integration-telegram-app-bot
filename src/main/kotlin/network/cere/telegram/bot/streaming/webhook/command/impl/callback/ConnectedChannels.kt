package network.cere.telegram.bot.streaming.webhook.command.impl.callback

import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.ddc.Wallet
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.webhook.BotProducer
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class ConnectedChannels(
    @ConfigProperty(name = "telegram.bot.username") private val botUsername: String,
    private val botProducer: BotProducer,
    private val json: Json,
    private val wallet: Wallet,
) : AbstractBotCallbackCommand {
    override fun command() = "/connectedChannels"

    override fun handle(update: Update) {
        val message = requireNotNull(update.callback_query?.message as Message)
        val from = requireNotNull(update.callback_query?.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        botProducer.sendTextMessage(
            message.chat.id,
            connectedChannelsText(user)
        )
    }

    fun connectedChannelsText(user: BotUser): String {
        val channelIds = user.channels.split(",").map { it.toLong() }
        val channels = Channel.list("id in ?1", channelIds)

        return """
                You have ${channels.size} channels connected:
                ${
            channels.joinToString( separator=""){ channel ->
                "\n" + """
                    ${channel.title}:
                        Viewer application: https://t.me/$botUsername/viewer?startapp=${channel.id}
                        Creator UI tool: https://t.me/$botUsername/creator?startapp=${channel.id}
                """.trimIndent()
            }
        }
                """
    }
}
