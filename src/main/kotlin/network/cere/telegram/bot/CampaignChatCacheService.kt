package network.cere.telegram.bot

import io.vertx.core.impl.logging.LoggerFactory
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.time.Instant
import kotlinx.serialization.json.*

@ApplicationScoped
class CampaignChatCacheService(
    @RestClient private val robClient: RobClient,
    private val config: Config,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var cache: Map<Long, CampaignContext> = emptyMap()
    private var lastUpdated: Instant = Instant.EPOCH
    private val cacheDuration = java.time.Duration.ofMinutes(1)


    fun getCampaignContextByChatId(chatId: Long): CampaignContext? {
        if (cache.isEmpty() || Instant.now().isAfter(lastUpdated.plus(cacheDuration))) {
            refreshCache()
        }
        return cache[chatId]
    }

    private fun refreshCache() {
        try {
            val organizationsResponse = robClient.getOrganizations(
                dataServiceId = config.appId(),
            )

            val organizations = organizationsResponse.data
            if (organizations.isEmpty()) {
                return
            }

            val allCampaigns = mutableListOf<Campaign>()
            for (organization in organizations) {
                val campaignsResponse = robClient.getCampaigns(
                    dataServiceId = config.appId(),
                    organizationId = organization.id
                )
                allCampaigns.addAll(campaignsResponse.data)
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
            val maxChannelRequestsPerDay = challengeSettingsObj?.get("maxChannelRequestsPerDay")?.jsonPrimitive?.intOrNull ?: 1000

            // Extract prompt tags
            val promptTagsArray = challengeSettingsObj?.get("promptTags")?.jsonArray
            val promptTags = if (promptTagsArray != null) {
                promptTagsArray.mapNotNull { element ->
                    if (element is JsonObject) {
                        val tag = element["tag"]?.jsonPrimitive?.content
                        val prompt = element["prompt"]?.jsonPrimitive?.content
                        if (tag != null && prompt != null) {
                            PromptTag(tag, prompt)
                        } else null
                    } else null
                }
            } else {
                listOf(
                    PromptTag("fire", "anime-style portrait of a man in a tuxedo with a blazing fire aura behind him, surrounded by heat waves and glowing embers, dramatic lighting, fiery background, highly detailed, cinematic look"),
                    PromptTag("ice", "anime-style portrait of a man in a tuxedo with a glowing icy aura behind him, surrounded by cold mist and blue light, dramatic lighting, frozen background, highly detailed, cinematic look")
                )
            }

            val challengeSettings = ChallengeSettings(
                cooldownHours = cooldown,
                maxImageGenerationPerDay = maxImageGeneration,
                maxBoostPerDay = maxBoost,
                maxChannelRequestsPerDay = maxChannelRequestsPerDay,
                promptTags = promptTags
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
        val maxChannelRequestsPerDay: Int = 1000,
        val promptTags: List<PromptTag> = listOf(
            PromptTag("fire", "anime-style portrait of a man in a tuxedo with a blazing fire aura behind him, surrounded by heat waves and glowing embers, dramatic lighting, fiery background, highly detailed, cinematic look"),
            PromptTag("ice", "anime-style portrait of a man in a tuxedo with a glowing icy aura behind him, surrounded by cold mist and blue light, dramatic lighting, frozen background, highly detailed, cinematic look")
        )
    )

    data class ChatChallengeConfig(
        val chatId: Long,
        val challengeSettings: ChallengeSettings
    )
}