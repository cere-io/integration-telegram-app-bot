package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class Campaign(
    @SerialName("campaignId")
    val campaignId: Int,
    val status: Int,
    @SerialName("startDate")
    val startDate: String,
    @SerialName("endDate")
    val endDate: String,
    @SerialName("campaignName")
    val campaignName: String? = null,
    val type: Int? = null,
    val archive: Int,
    val mobile: Int? = null,
    @SerialName("userName")
    val userName: String? = null,
    @SerialName("modDate")
    val modDate: String? = null,
    val guid: String? = null,
    @SerialName("formData")
    val formData: String? = null,
    @SerialName("appId")
    val appId: String? = null,
) {
    fun parseCampaignDetails(): CampaignDetails? {
        return if (formData != null) {
            try {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<CampaignFormData>(formData).campaign
            } catch (e: Exception) {
                println("Failed to parse formData: ${e.message}")
                println("FormData content: $formData")
                null
            }
        } else null
    }
    
    fun isActive(): Boolean {
        if (status != 1 || archive != 0) {
            return false
        }
        
        return try {
            val now = Instant.now()
            val start = Instant.parse(startDate)
            val end = Instant.parse(endDate)
            now.isAfter(start) && now.isBefore(end)
        } catch (e: Exception) {
            // If date parsing fails, fall back to status/archive check
            status == 1 && archive == 0
        }
    }
    
    fun getDescription(): String {
        // Try to get description from formData first
        val details = parseCampaignDetails()
        return details?.description ?: "No description available"
    }
    
    fun getDateRange(): String {
        // Try to get date range from formData first, fallback to top-level fields
        val details = parseCampaignDetails()
        if (details != null) {
            val formattedRange = details.formatDateRange()
            if (formattedRange != "Date range not available") {
                return formattedRange
            }
        }
        
        // Fallback to top-level startDate and endDate
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val start = Instant.parse(startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val end = Instant.parse(endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            "${start.format(formatter)} - ${end.format(formatter)}"
        } catch (e: Exception) {
            "Date range not available"
        }
    }
    
    fun getStatusText(): String {
        return when {
            isActive() -> "Active"
            isEnded() -> "Ended"
            isUpcoming() -> "Upcoming"
            else -> "Inactive"
        }
    }
    
    fun isEnded(): Boolean {
        return try {
            val now = Instant.now()
            val end = Instant.parse(endDate)
            now.isAfter(end)
        } catch (e: Exception) {
            false
        }
    }
    
    fun isUpcoming(): Boolean {
        return try {
            val now = Instant.now()
            val start = Instant.parse(startDate)
            now.isBefore(start) && status == 1 && archive == 0
        } catch (e: Exception) {
            false
        }
    }
}

@Serializable
data class CampaignFormData(
    val campaign: CampaignDetails,
)

@Serializable
data class CampaignDetails(
    val description: String,
    val endDate: String,
    val name: String,
    val startDate: String,
    val status: String,
    val debug: Boolean = false,
    val disableQuests: Boolean = false,
) {
    fun formatDateRange(): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            val start = Instant.parse(startDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val end = Instant.parse(endDate).atZone(ZoneId.systemDefault()).toLocalDateTime()
            "${start.format(formatter)} - ${end.format(formatter)}"
        } catch (e: Exception) {
            "Date range not available"
        }
    }
} 
