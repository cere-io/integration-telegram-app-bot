package network.cere.telegram.bot

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.ParseMode
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class PrivateMessageHandler(
    @RestClient private val robClient: RobClient,
    @RestClient private val botApi: BotApi,
    private val config: Config,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val text = message.text ?: return
        
        if (!text.startsWith("/start")) {
            return
        }
        
        val chatId = chat.id.longValue
        
        // Parse the deep link parameter
        val startParam = text.substringAfter("/start").trim()
        if (startParam.isEmpty()) {
            sendWelcomeMessage(chatId)
            return
        }
        
        try {
            when {
                startParam.startsWith("org_") -> handleOrganizationLink(chatId, startParam.substring(4))
                startParam.startsWith("cmp_") -> handleCampaignLink(chatId, startParam.substring(4))
                else -> sendWelcomeMessage(chatId)
            }
        } catch (e: Exception) {
            log.error("Error handling deep link: $startParam", e)
            sendErrorMessage(chatId)
        }
    }
    
    private fun handleOrganizationLink(chatId: Long, orgReference: String) {
        log.info("Handling organization link: $orgReference")
        
        val organizationId = if (orgReference.all { it.isDigit() }) {
            orgReference
        } else {
            // Handle slug - for now, we'll need to fetch campaigns to find the org
            // This is a simplified implementation
            log.warn("Organization slug handling not fully implemented: $orgReference")
            sendErrorMessage(chatId)
            return
        }
        
        val campaignsResponse = robClient.getCampaigns(
            dataServiceId = config.appId(),
            organizationId = organizationId
        )
        
        val activeCampaigns = campaignsResponse.data.filter { it.isActive() }
        
        when (activeCampaigns.size) {
            0 -> sendNoCampaignsMessage(chatId)
            1 -> sendCampaignDetails(chatId, activeCampaigns.first())
            else -> sendCampaignSelection(chatId, activeCampaigns)
        }
    }
    
    private fun handleCampaignLink(chatId: Long, campaignReference: String) {
        log.info("Handling campaign link: $campaignReference")
        
        val campaignId = if (campaignReference.all { it.isDigit() }) {
            campaignReference
        } else {
            // Handle slug - for now, we'll need to fetch campaigns to find by name
            log.warn("Campaign slug handling not fully implemented: $campaignReference")
            sendErrorMessage(chatId)
            return
        }
        
        val campaignResponse = robClient.getCampaign(campaignId)
        val campaign = campaignResponse.data
        
        if (campaign.isActive()) {
            sendCampaignDetails(chatId, campaign)
        } else {
            sendInactiveCampaignMessage(chatId)
        }
    }
    
    private fun sendCampaignDetails(chatId: Long, campaign: Campaign) {
        val details = campaign.parseCampaignDetails()
        val name = details?.name ?: campaign.campaignName ?: "Campaign"
        val description = details?.description ?: "No description available"
        val dateRange = details?.formatDateRange() ?: "Date range not available"
        
        val message = """
            🎯 **$name**
            
            $description
            
            📅 **Duration:** $dateRange
            
            Ready to join the campaign? Tap the button below to get started!
            
            Launch the mini app: https://t.me/ulad_bullish_bot/viewer?startapp=${campaign.campaignId}
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown
        )
        
        botApi.sendMessage(request)
        log.info("Sent campaign details for campaign ${campaign.campaignId}")
    }
    
    private fun sendCampaignSelection(chatId: Long, campaigns: List<Campaign>) {
        val message = """
            🎯 **Choose a Campaign**
            
            Select which campaign you'd like to participate in:
        """.trimIndent()
        
        val campaignList = campaigns.mapIndexed { index, campaign ->
            val details = campaign.parseCampaignDetails()
            val name = details?.name ?: campaign.campaignName ?: "Campaign ${campaign.campaignId}"
            "${index + 1}. $name"
        }.joinToString("\n")
        
        val fullMessage = "$message\n\n$campaignList\n\nTo start a campaign, use the mini app: https://t.me/ulad_bullish_bot/viewer?startapp=CAMPAIGN_ID"
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = fullMessage,
            parse_mode = ParseMode.Markdown
        )
        
        botApi.sendMessage(request)
        log.info("Sent campaign selection with ${campaigns.size} campaigns")
    }
    
    private fun sendWelcomeMessage(chatId: Long) {
        val message = """
            👋 **Welcome to Bullish Bot!**
            
            This bot helps you participate in exciting marketing campaigns. 
            
            To get started, you'll need a campaign link from your organization.
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown
        )
        
        botApi.sendMessage(request)
        log.info("Sent welcome message")
    }
    
    private fun sendNoCampaignsMessage(chatId: Long) {
        val message = """
            😔 **No Active Campaigns**
            
            There are currently no active campaigns available for this organization.
            
            Please check back later or contact the organization for more information.
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown
        )
        
        botApi.sendMessage(request)
        log.info("Sent no campaigns message")
    }
    
    private fun sendInactiveCampaignMessage(chatId: Long) {
        val message = """
            ⚠️ **Campaign Not Available**
            
            This campaign is currently not active or has ended.
            
            Please check with your organization for active campaigns.
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown
        )
        
        botApi.sendMessage(request)
        log.info("Sent inactive campaign message")
    }
    
    private fun sendErrorMessage(chatId: Long) {
        val message = """
            ❌ **Something went wrong**
            
            We couldn't process your request. Please try again later or contact support.
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown
        )
        
        botApi.sendMessage(request)
        log.info("Sent error message")
    }
} 