package network.cere.telegram.bot.streaming.webhook.command.impl.callback

import com.github.omarmiatello.telegram.*
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
                "Share the group you want to read messages from. You must be an admin in this group.",
                ReplyKeyboardMarkup(
                        resize_keyboard = true,
                        keyboard =
                                listOf(
                                        listOf(
                                                KeyboardButton(
                                                        text = "Share group",
                                                        request_chat =
                                                                KeyboardButtonRequestChat(
                                                                        request_id =
                                                                                101L, // Different
                                                                        // from
                                                                        // channel
                                                                        // request ID
                                                                        chat_is_channel = false,
                                                                        chat_is_forum = false,
                                                                        chat_has_username = true,
                                                                        chat_is_created = true,
                                                                        bot_is_member = true,
                                                                        user_administrator_rights =
                                                                                ChatAdministratorRights(
                                                                                        is_anonymous =
                                                                                                false,
                                                                                        can_manage_chat =
                                                                                                true,
                                                                                        can_delete_messages =
                                                                                                true,
                                                                                        can_manage_video_chats =
                                                                                                true,
                                                                                        can_restrict_members =
                                                                                                true,
                                                                                        can_promote_members =
                                                                                                true,
                                                                                        can_change_info =
                                                                                                true,
                                                                                        can_invite_users =
                                                                                                true,
                                                                                        can_post_messages =
                                                                                                false,
                                                                                        can_edit_messages =
                                                                                                false,
                                                                                        can_pin_messages =
                                                                                                true,
                                                                                        can_post_stories =
                                                                                                false,
                                                                                        can_edit_stories =
                                                                                                false,
                                                                                        can_delete_stories =
                                                                                                false,
                                                                                )
                                                                )
                                                )
                                        )
                                )
                )
        )
    }
}
