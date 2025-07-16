package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class GroupSubscriptionTracker {
    private val log = LoggerFactory.getLogger(javaClass)
    
    // Thread-safe set to store active group chat IDs
    private val subscribedGroups = ConcurrentHashMap.newKeySet<String>()
    
    /**
     * Add a group to the subscription list (when bot receives messages from it)
     */
    fun addGroup(chatId: Long, chatTitle: String? = null) {
        val chatIdStr = chatId.toString()
        val wasNew = subscribedGroups.add(chatIdStr)
        
        if (wasNew) {
            log.info("GroupSubscriptionTracker.kt: ✅ Added new group subscription: {} ({})", 
                chatIdStr, chatTitle ?: "Unknown")
        } else {
            log.debug("GroupSubscriptionTracker.kt: 🔄 Group already tracked: {} ({})", 
                chatIdStr, chatTitle ?: "Unknown")
        }
    }
    
    /**
     * Remove a group from subscription list (when bot is removed)
     */
    fun removeGroup(chatId: Long, chatTitle: String? = null) {
        val chatIdStr = chatId.toString()
        val wasRemoved = subscribedGroups.remove(chatIdStr)
        
        if (wasRemoved) {
            log.info("GroupSubscriptionTracker.kt: ❌ Removed group subscription: {} ({})", 
                chatIdStr, chatTitle ?: "Unknown")
        } else {
            log.debug("GroupSubscriptionTracker.kt: ⚠️ Attempted to remove non-tracked group: {} ({})", 
                chatIdStr, chatTitle ?: "Unknown")
        }
    }
    
    /**
     * Get all currently subscribed group IDs
     */
    fun getSubscribedGroups(): List<String> {
        val groups = subscribedGroups.toList()
        log.debug("GroupSubscriptionTracker.kt: 📋 Current subscribed groups: {} (count: {})", 
            groups, groups.size)
        return groups
    }
    
    /**
     * Check if bot is subscribed to a specific group
     */
    fun isSubscribedTo(chatId: Long): Boolean {
        return subscribedGroups.contains(chatId.toString())
    }
    
    /**
     * Get subscription count
     */
    fun getSubscriptionCount(): Int {
        return subscribedGroups.size
    }
    
    /**
     * Clear all subscriptions (useful for testing)
     */
    fun clearAll() {
        val count = subscribedGroups.size
        subscribedGroups.clear()
        log.warn("GroupSubscriptionTracker.kt: 🗑️ Cleared all {} group subscriptions", count)
    }
    
    /**
     * Get subscription info for logging/debugging
     */
    fun getSubscriptionInfo(): Map<String, Any> {
        return mapOf(
            "subscribedGroupCount" to subscribedGroups.size,
            "subscribedGroups" to subscribedGroups.toList()
        )
    }
} 