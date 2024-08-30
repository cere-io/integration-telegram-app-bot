package network.cere.telegram.bot.streaming.webhook

import com.github.omarmiatello.telegram.*
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.jsonObject
import network.cere.telegram.bot.api.BotApi
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import kotlin.random.Random

@ApplicationScoped
class BotConsumer(private val botProducer: BotProducer, @RestClient private val botApi: BotApi) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleUpdate(update: Update) {
        when {
            isBotCommand(update) -> botProducer.sendTextMessage(
                extractChatId(update), "Some intro text here", ReplyKeyboardMarkup(
                    resize_keyboard = true,
                    keyboard = listOf(
                        listOf(
                            KeyboardButton(
                                text = "Select channel",
                                request_chat = KeyboardButtonRequestChat(
                                    request_id = Random.nextLong(1, Int.MAX_VALUE.toLong()), //TODO generate properly
                                    chat_is_channel = true,
                                    chat_is_created = true,
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
            isChatShared(update) -> {
                val sharedChatId = ChatId(requireNotNull(update.message?.chat_shared).chat_id.stringValue)
                val chatInfo = botApi.getChat(TelegramRequest.GetChatRequest(sharedChatId))
                botProducer.sendTextMessage(extractChatId(update), "Ok, let's configure channel ${chatInfo.result?.jsonObject?.getValue("title")}", InlineKeyboardMarkup(
                    inline_keyboard = listOf(
                        listOf(
                            InlineKeyboardButton(text = "Set Bucket ID", callback_data = "/setBucket ${sharedChatId.stringValue}"),
                            InlineKeyboardButton(text = "Set Bot Access Token", callback_data = "/setToken ${sharedChatId.stringValue}")
                        ),
                        listOf(
                            InlineKeyboardButton(text = "Set payouts address", callback_data = "/setPayoutsAddress ${sharedChatId.stringValue}"),
                            InlineKeyboardButton(text = "Configure subscription levels", callback_data = "/configureSubscriptions ${sharedChatId.stringValue}")
                        ),
                        listOf(
                            InlineKeyboardButton(text = "Check configuration", callback_data = "/check ${sharedChatId.stringValue}"),
                        )
                    )
                ))
            }
            else -> log.info("Unknown command")
        }
    }

    private fun isAddedToTheChannel(update: Update): Boolean {
        if (update.my_chat_member == null) {
            return false
        }
        val myChatMember = requireNotNull(update.my_chat_member)
        if (myChatMember.chat.type != "channel") {
            return false
        }
        return when (val newChatMember = myChatMember.new_chat_member) {
            is ChatMemberUpdated -> false
            is ChatMemberOwner -> newChatMember.status == "administrator" && newChatMember.user.id.longValue == 7295860533L
            is ChatMemberAdministrator -> newChatMember.status == "administrator" && newChatMember.user.id.longValue == 7295860533L
            is ChatMemberMember -> newChatMember.status == "administrator" && newChatMember.user.id.longValue == 7295860533L
            is ChatMemberRestricted -> newChatMember.status == "administrator" && newChatMember.user.id.longValue == 7295860533L
            is ChatMemberLeft -> newChatMember.status == "administrator" && newChatMember.user.id.longValue == 7295860533L
            is ChatMemberBanned -> newChatMember.status == "administrator" && newChatMember.user.id.longValue == 7295860533L
        }
    }
    
    private fun isBotCommand(update: Update): Boolean {
        if (update.message == null) {
            return false
        }
        val msg = requireNotNull(update.message)
        return msg.entities.orEmpty().any { it.type == "bot_command" }
    }
    
    private fun isChatShared(update: Update): Boolean {
        return update.message?.chat_shared != null
    }

    private fun isMessageToBot(update: Update): Boolean {
        return update.message != null
    }

    private fun extractChatId(update: Update): ChatId {
        return when {
            update.my_chat_member != null -> requireNotNull(update.my_chat_member).chat.id
            update.message != null -> requireNotNull(update.message).chat.id
            else -> throw UnsupportedOperationException("Unable to extract Chat ID from message $update")
        }
    }
}