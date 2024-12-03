package network.cere.telegram.bot.streaming.webhook.command.impl.menu

import com.github.omarmiatello.telegram.*
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.command.impl.callback.ConnectedChannels
import network.cere.telegram.bot.streaming.webhook.command.impl.share.ShareChannel

@ApplicationScoped
class Start(
    private val botProducer: BotProducer,
    private val shareChannel: ShareChannel,
    private val json: Json,
    private val connectedChannels: ConnectedChannels,
) : AbstractBotMenuCommand {
    override fun menuOrder() = 10

    override fun description() = "Start working with a bot"

    override fun command() = "/start"

    override fun handle(update: Update) {
        val chatId = requireNotNull(update.message).chat.id
        val from = requireNotNull(update.message?.from)
        val user = BotUser.findById(from.id.longValue) ?: BotUser(
            id = from.id.longValue,
            isBot = from.is_bot,
            firstName = from.first_name,
            chatContextJson = json.encodeToString(ChatContext()),
        ).also { it.persistAndFlush() }

        botProducer.sendTextMessage(
            chatId,
            connectedChannels.connectedChannelsText(user)
        )

        botProducer.sendTextMessage(
            requireNotNull(update.message).chat.id,
            "Connect the channel you want to use. You must have admin privileges in this channel.",
            ReplyKeyboardMarkup(
                resize_keyboard = true,
                keyboard = listOf(
                    listOf(
                        KeyboardButton(
                            text = shareChannel.command(),
                            request_chat = KeyboardButtonRequestChat(
                                request_id = shareChannel.requestId(),
                                chat_is_channel = true,
                                chat_is_created = true,
                                bot_is_member = true,
                                user_administrator_rights = ChatAdministratorRights(
                                    is_anonymous = false,
                                    can_manage_chat = true,
                                    can_delete_messages = true,
                                    can_manage_video_chats = true,
                                    can_restrict_members = true,
                                    can_promote_members = true,
                                    can_change_info = true,
                                    can_invite_users = true,
                                    can_post_stories = true,
                                    can_edit_stories = true,
                                    can_delete_stories = true,
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
