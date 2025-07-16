package network.cere.telegram.bot

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.ParseMode
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class ChatNotificationService(
    @RestClient private val botApi: BotApi,
    private val groupSubscriptionTracker: GroupSubscriptionTracker,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class NotificationResult(
        val successCount: Int,
        val errors: List<String>
    )

    fun sendToChats(
        message: String,
        targetChats: List<String>? = null
    ): NotificationResult {
        val chatIds = determineChatIds(targetChats)
        val chatInfo = chatIds.map { "$it (${getChatTypeInfo(it)})" }
        log.info("Sending notification to {} chats: {}", chatIds.size, chatInfo)

        val errors = mutableListOf<String>()
        var successCount = 0

        chatIds.forEach { chatId ->
            val chatType = getChatTypeInfo(chatId)
            try {
                sendMessageToChat(chatId, message)
                successCount++
                log.debug("Successfully sent message to {} chat: {}", chatType, chatId)
            } catch (e: Exception) {
                val errorMessage = "Chat $chatId ($chatType): ${e.message ?: "Unknown error"}"
                errors.add(errorMessage)
                log.error("Failed to send message to {} chat {}: {}", chatType, chatId, e.message, e)
            }
        }

        log.info("Notification sent to {}/{} chats successfully", successCount, chatIds.size)
        return NotificationResult(successCount, errors)
    }

    private fun determineChatIds(targetChats: List<String>?): List<String> {
        return if (targetChats.isNullOrEmpty()) {
            // Use dynamically discovered subscribed groups
            val subscribedGroups = groupSubscriptionTracker.getSubscribedGroups()
            log.debug("ChatNotificationService.kt: Using dynamically discovered groups: {} (count: {})", 
                subscribedGroups, subscribedGroups.size)
            
            if (subscribedGroups.isEmpty()) {
                log.warn("ChatNotificationService.kt: ⚠️ No subscribed groups found! Bot may not be added to any groups yet.")
            }
            
            subscribedGroups
        } else {
            log.debug("ChatNotificationService.kt: Using specified target chats (groups and/or private): {}", targetChats)
            targetChats
        }
    }

    private fun sendMessageToChat(chatId: String, message: String) {
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId),
            text = message,
            parse_mode = ParseMode.Markdown,
            disable_web_page_preview = true
        )

        try {
            val response = botApi.sendMessage(request)
            log.debug("Telegram API response for chat {}: {}", chatId, response)
        } catch (e: Exception) {
            log.error("Failed to send message to chat {}", chatId, e)
            throw e
        }
    }

    fun getConfiguredChatIds(): List<String> {
        return groupSubscriptionTracker.getSubscribedGroups()
    }

    fun getChatTypeInfo(chatId: String): String {
        return when {
            chatId.startsWith("-100") -> "Supergroup"
            chatId.startsWith("-") -> "Group" 
            chatId.toLongOrNull()?.let { it > 0 } == true -> "Private"
            else -> "Unknown"
        }
    }
} 