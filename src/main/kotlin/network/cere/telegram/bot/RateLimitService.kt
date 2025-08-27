package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class RateLimitService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channelRequestStats = ConcurrentHashMap<Long, ChannelRequestStats>()
    private val userRequestStats = ConcurrentHashMap<Long, UserRequestStats>()

    data class ChannelRequestStats(
        val channelId: Long,
        val dailyRequests: MutableMap<LocalDate, Int> = mutableMapOf(),
        val hourlyRequests: MutableMap<LocalDateTime, Int> = mutableMapOf(),
    )

    data class UserRequestStats(
        val userId: Long,
        val imageGenerationRequests: MutableMap<LocalDate, Int> = mutableMapOf(),
        val boostRequests: MutableMap<LocalDate, Int> = mutableMapOf(),
        val funCommandUsage: MutableMap<LocalDateTime, Boolean> = mutableMapOf()
    )

    fun canMakeChannelRequest(channelId: Long, limits: CampaignChatCacheService.ChallengeSettings): Boolean {
        val stats = getOrCreateChannelStats(channelId)
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        cleanupOldChannelStats(stats, now, today)
        val dailyCount = stats.dailyRequests.getOrDefault(today, 0)
        val canMake = dailyCount < limits.maxChannelRequestsPerDay

        if (!canMake) {
            log.warn("Channel rate limit exceeded for channel $channelId: daily=$dailyCount/${limits.maxChannelRequestsPerDay}")
        }
        return canMake
    }

    fun canGenerateImage(userId: Long, limits: CampaignChatCacheService.ChallengeSettings): Boolean {
        val stats = getOrCreateUserStats(userId)
        val today = LocalDate.now()

        cleanupOldUserStats(stats, today)
        val dailyCount = stats.imageGenerationRequests.getOrDefault(today, 0)
        val canMake = dailyCount < limits.maxImageGenerationPerDay

        if (!canMake) {
            log.warn("Image generation limit exceeded for user $userId: daily=$dailyCount/${limits.maxImageGenerationPerDay}")
        }
        return canMake
    }

    fun canBoost(userId: Long, limits: CampaignChatCacheService.ChallengeSettings): Boolean {
        val stats = getOrCreateUserStats(userId)
        val today = LocalDate.now()

        cleanupOldUserStats(stats, today)
        val dailyCount = stats.boostRequests.getOrDefault(today, 0)
        val canMake = dailyCount < limits.maxBoostPerDay

        if (!canMake) {
            log.warn("Boost limit exceeded for user $userId: daily=$dailyCount/${limits.maxBoostPerDay}")
        }
        return canMake
    }

    fun recordChannelRequest(channelId: Long) {
        val stats = getOrCreateChannelStats(channelId)
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        stats.dailyRequests[today] = stats.dailyRequests.getOrDefault(today, 0) + 1
        stats.hourlyRequests[now.withMinute(0).withSecond(0).withNano(0)] =
            stats.hourlyRequests.getOrDefault(now.withMinute(0).withSecond(0).withNano(0), 0) + 1

        log.debug("Channel request recorded for channel $channelId")
    }

    fun recordImageGeneration(userId: Long) {
        val stats = getOrCreateUserStats(userId)
        val today = LocalDate.now()
        stats.imageGenerationRequests[today] = stats.imageGenerationRequests.getOrDefault(today, 0) + 1
        log.debug("Image generation recorded for user $userId")
    }

    fun recordBoost(userId: Long) {
        val stats = getOrCreateUserStats(userId)
        val today = LocalDate.now()
        stats.boostRequests[today] = stats.boostRequests.getOrDefault(today, 0) + 1
        log.debug("Boost recorded for user $userId")
    }

    fun canUseFun(userId: Long, cooldownHours: Long): Boolean {
        val stats = getOrCreateUserStats(userId)
        val now = LocalDateTime.now()

        stats.funCommandUsage.entries.removeIf { it.key.plusHours(cooldownHours).isBefore(now) }
        val lastUsage = stats.funCommandUsage.keys.maxOrNull()
        val canUse = lastUsage == null || lastUsage.plusHours(cooldownHours).isBefore(now)

        if (!canUse) {
            log.warn("Fun command cooldown active for user $userId, last used at $lastUsage")
        }
        return canUse
    }

    fun recordFunUsage(userId: Long) {
        val stats = getOrCreateUserStats(userId)
        val now = LocalDateTime.now()
        stats.funCommandUsage[now] = true
        log.debug("Fun command usage recorded for user $userId at $now")
    }

    private fun getOrCreateChannelStats(channelId: Long): ChannelRequestStats {
        return channelRequestStats.computeIfAbsent(channelId) { ChannelRequestStats(it) }
    }

    private fun getOrCreateUserStats(userId: Long): UserRequestStats {
        return userRequestStats.computeIfAbsent(userId) { UserRequestStats(it) }
    }

    private fun cleanupOldChannelStats(stats: ChannelRequestStats, now: LocalDateTime, today: LocalDate) {
        stats.dailyRequests.entries.removeIf { it.key.isBefore(today) }
        val dayAgo = now.minusHours(24)
        stats.hourlyRequests.entries.removeIf { it.key.isBefore(dayAgo) }
    }

    private fun cleanupOldUserStats(stats: UserRequestStats, today: LocalDate) {
        stats.imageGenerationRequests.entries.removeIf { it.key.isBefore(today) }
        stats.boostRequests.entries.removeIf { it.key.isBefore(today) }
        stats.funCommandUsage.entries.removeIf { it.key.isBefore(today.atStartOfDay()) }
    }
}