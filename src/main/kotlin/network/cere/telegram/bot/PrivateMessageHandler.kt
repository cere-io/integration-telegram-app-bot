package network.cere.telegram.bot

import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.ParseMode
import com.github.omarmiatello.telegram.InlineKeyboardMarkup
import com.github.omarmiatello.telegram.InlineKeyboardButton
import com.github.omarmiatello.telegram.WebAppInfo
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
            sendEndedCampaignMessage(chatId, campaign)
        }
    }
    
    private fun sendCampaignDetails(chatId: Long, campaign: Campaign) {
        val details = campaign.parseCampaignDetails()
        val name = details?.name ?: campaign.campaignName ?: "Campaign"
        val description = campaign.getDescription()
        val dateRange = campaign.getDateRange()
        
        val message = """
            🎯 **$name**
            
            $description
            
            📅 **Duration:** $dateRange
            
            Ready to join the campaign? Tap the button below to get started!
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown,
            reply_markup = createWebAppButton(campaign.campaignId, "🚀 Join Campaign")
        )
        
        sendMessageSafely(request, "campaign details for campaign ${campaign.campaignId}")
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
        
        val fullMessage = "$message\n\n$campaignList\n\nSelect a campaign using the buttons below:"
        
        val buttons = campaigns.map { campaign ->
            val details = campaign.parseCampaignDetails()
            val name = details?.name ?: campaign.campaignName ?: "Campaign ${campaign.campaignId}"
            val buttonText = "🎯 $name"
            InlineKeyboardButton(
                text = buttonText,
                web_app = WebAppInfo(url = "https://t.me/${config.botName()}/${config.miniAppName()}?startapp=${campaign.campaignId}")
            )
        }
        
        val keyboard = InlineKeyboardMarkup(inline_keyboard = buttons.chunked(1))
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = fullMessage,
            parse_mode = ParseMode.Markdown,
            reply_markup = keyboard
        )
        
        sendMessageSafely(request, "campaign selection with ${campaigns.size} campaigns")
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
        
        sendMessageSafely(request, "welcome message")
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
        
        sendMessageSafely(request, "no campaigns message")
    }
    
    private fun sendEndedCampaignMessage(chatId: Long, campaign: Campaign) {
        val details = campaign.parseCampaignDetails()
        val name = details?.name ?: campaign.campaignName ?: "Campaign"
        
        val message = """
            🏁 **Campaign Ended: $name**
            
            This campaign has ended, but you can still check your results!
            
            • View your earned points
            • Check your leaderboard position
            • See your completed quests
            
            Tap the button below to view your results:
        """.trimIndent()
        
        val request = TelegramRequest.SendMessageRequest(
            chat_id = ChatId(chatId.toString()),
            text = message,
            parse_mode = ParseMode.Markdown,
            reply_markup = createWebAppButton(campaign.campaignId, "📊 View Results")
        )
        
        sendMessageSafely(request, "ended campaign message")
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
        
        sendMessageSafely(request, "error message")
    }
    
    private fun sendMessageSafely(request: TelegramRequest.SendMessageRequest, messageType: String) {
        try {
            val response = botApi.sendMessage(request)
            log.info("Sent $messageType successfully")
            log.debug("Telegram API response: $response")
        } catch (e: Exception) {
            log.error("Failed to send $messageType", e)
        }
    }
    
    private fun escapeMarkdown(text: String): String {
        return text.replace("_", "\\_")
                  .replace("*", "\\*")
                  .replace("[", "\\[")
                  .replace("]", "\\]")
                  .replace("(", "\\(")
                  .replace(")", "\\)")
                  .replace("~", "\\~")
                  .replace("`", "\\`")
                  .replace(">", "\\>")
                  .replace("#", "\\#")
                  .replace("+", "\\+")
                  .replace("-", "\\-")
                  .replace("=", "\\=")
                  .replace("|", "\\|")
                  .replace("{", "\\{")
                  .replace("}", "\\}")
                  .replace(".", "\\.")
                  .replace("!", "\\!")
    }
    
    private fun createWebAppButton(campaignId: Int, buttonText: String = "🚀 Launch Campaign"): InlineKeyboardMarkup {
        val webAppUrl = "https://t.me/${config.botName()}/${config.miniAppName()}?startapp=$campaignId"
        val button = InlineKeyboardButton(
            text = buttonText,
            url = webAppUrl
        )
        return InlineKeyboardMarkup(inline_keyboard = listOf(listOf(button)))
    }
}
