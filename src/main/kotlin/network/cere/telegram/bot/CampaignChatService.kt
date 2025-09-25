package network.cere.telegram.bot

import io.vertx.core.impl.logging.LoggerFactory
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.Instant
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

@ApplicationScoped
class CampaignChatService(
    @RestClient private val robClient: RobClient,
    private val config: Config,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private var cache: Map<Long, CampaignContext> = emptyMap()
    private var lastUpdated: Instant = Instant.EPOCH
    private val cacheDuration = java.time.Duration.ofMinutes(1)

    private val funCommandUserMap = ConcurrentHashMap<Long, MutableSet<Long>>()

    fun saveFunCommandUserId(chatId: Long, userId: Long) {
        funCommandUserMap.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(userId)
        log.info("Saved fun command userId=$userId for chatId=$chatId")
    }

    fun isUserAllowedToUploadImage(chatId: Long, userId: Long): Boolean {
        return funCommandUserMap[chatId]?.contains(userId) == true
    }

    fun clearFunCommandUserId(chatId: Long, userId: Long) {
        funCommandUserMap[chatId]?.remove(userId)
        log.info("Cleared fun command userId=$userId for chatId=$chatId")
    }

    fun getCampaignContextByChatId(chatId: Long): CampaignContext? {
        if (cache.isEmpty() || Instant.now().isAfter(lastUpdated.plus(cacheDuration))) {
            refreshCache()
        }
        return cache[chatId]
    }

    private fun refreshCache() {
        try {
            val staticConfigs = config.groups().values
            val orgIds = staticConfigs.map { it.orgId() }.distinct()
            val campaignIds = staticConfigs.map { it.campaignId() }.distinct()


            if (orgIds.isEmpty()) {
                log.warn("No organizations found in static config")
                return
            }

            val allCampaigns = mutableListOf<Campaign>()
            for (orgId in orgIds) {
                val campaignsResponse = robClient.getCampaigns(
                    dataServiceId = config.appId(),
                    organizationId = orgId.toString()
                )
                val filteredCampaigns = campaignsResponse.data.filter { campaign ->
                    campaign.campaignId.toString() in campaignIds
                }
                allCampaigns.addAll(filteredCampaigns)
            }

            val newMap = allCampaigns.flatMap { campaign ->
                val formData = campaign.formData
                val chatConfigs: List<ChatChallengeConfig> = extractChatIdsFromFormData(formData)
                chatConfigs.map { chatConfig ->
                    chatConfig.chatId to CampaignContext(
                        orgId = campaign.appId?.toLongOrNull() ?: 0L,
                        campaignId = campaign.campaignId.toLong(),
                        campaignName = campaign.campaignName ?: "Unknown Campaign",
                        challengeSettings = chatConfig.challengeSettings
                    )
                }
            }.toMap()

            cache = newMap
            lastUpdated = Instant.now()
            log.info("Cache refreshed successfully. Loaded ${newMap.size} chat configurations with challenge settings")
        } catch (e: Exception) {
            log.error("❌ Failed to refresh campaign-chat mapping", e)
        }
    }

    private fun extractChatIdsFromFormData(formData: String?): List<ChatChallengeConfig> {
        if (formData == null) return emptyList()

        return try {
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(formData).jsonObject

            val chatField = root["telegramChannels"] ?: return emptyList()
            val challengeSettingsObj = root["challengeSettings"]?.jsonObject

            val cooldown = challengeSettingsObj?.get("cooldownHours")?.jsonPrimitive?.intOrNull ?: 24
            val maxImageGeneration = challengeSettingsObj?.get("maxImageGenerationPerDay")?.jsonPrimitive?.intOrNull ?: 1
            val maxBoost = challengeSettingsObj?.get("maxBoostPerDay")?.jsonPrimitive?.intOrNull ?: 1
            val maxChannelRequestsPerDay = challengeSettingsObj?.get("maxChannelRequestsPerDay")?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE

            val challengeSettings = ChallengeSettings(
                cooldownHours = cooldown,
                maxImageGenerationPerDay = maxImageGeneration,
                maxBoostPerDay = maxBoost,
                maxChannelRequestsPerDay = maxChannelRequestsPerDay
            )

            return when (chatField) {
                is JsonArray -> chatField.mapNotNull { element ->
                    if (element is JsonObject) {
                        val idElement = element["id"]
                        if (idElement is JsonPrimitive) {
                            idElement.longOrNull?.let { chatId ->
                                ChatChallengeConfig(
                                    chatId = chatId,
                                    challengeSettings = challengeSettings
                                )
                            }
                        } else null
                    } else null
                }
                is JsonPrimitive -> chatField.longOrNull?.let { chatId ->
                    listOf(ChatChallengeConfig(
                        chatId = chatId,
                        challengeSettings = challengeSettings
                    ))
                } ?: emptyList()
                else -> emptyList()
            }
        } catch (e: Exception) {
            log.warn("Failed to parse formData: $formData", e)
            emptyList()
        }
    }

    data class CampaignContext(
        val orgId: Long,
        val campaignId: Long,
        val campaignName: String,
        val challengeSettings: ChallengeSettings
    )

    data class ChallengeSettings(
        val cooldownHours: Int,
        val maxImageGenerationPerDay: Int = 1,
        val maxBoostPerDay: Int = 1,
        val maxChannelRequestsPerDay: Int = Int.MAX_VALUE
    )

    data class ChatChallengeConfig(
        val chatId: Long,
        val challengeSettings: ChallengeSettings
    )
}