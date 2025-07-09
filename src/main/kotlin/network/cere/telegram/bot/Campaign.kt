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
                Json.decodeFromString<CampaignFormData>(formData).campaign
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    fun isActive(): Boolean {
        return status == 1 && archive == 0
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
