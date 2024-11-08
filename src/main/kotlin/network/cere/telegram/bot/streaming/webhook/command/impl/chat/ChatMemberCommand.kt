package network.cere.telegram.bot.streaming.webhook.command.impl.chat

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import network.cere.telegram.bot.api.BotApi
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.webhook.command.BotCommand
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class ChatMemberCommand(@RestClient private val botApi: BotApi) : BotCommand {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun command() = "chat_member"

    override fun handle(update: Update) {
        val chat = requireNotNull(update.chat_member).chat
        runCatching {
            val memberCount = botApi.getChatMemberCount(TelegramRequest.GetChatMemberCountRequest(chat.id)).result
            Channel.update("memberCount = ?1 where id = ?2", memberCount!!, chat.id.longValue)
        }.onFailure {
            log.info(
                "Unable to get chat member count (id={}, title={}, username={})",
                chat.id,
                chat.title,
                chat.username
            )
        }
    }
}
