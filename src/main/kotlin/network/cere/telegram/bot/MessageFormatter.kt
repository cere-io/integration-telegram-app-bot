package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@ApplicationScoped
class MessageFormatter {
    private val log = LoggerFactory.getLogger(javaClass)

    fun formatMessage(
        analysis: PayloadAnalyzer.AnalysisResult,
        source: String? = null,
        priority: PayloadPriority = PayloadPriority.NORMAL
    ): String {
        log.debug("Formatting message for type: {}, priority: {}, source: {}", analysis.type, priority, source)

        val priorityPrefix = when (priority) {
            PayloadPriority.URGENT -> "🚨 **URGENT** "
            PayloadPriority.HIGH -> "⚠️ **HIGH PRIORITY** "
            else -> ""
        }

        val sourceInfo = source?.let { "\n📤 **Source:** `$it`" } ?: ""
        
        val formattedContent = when (analysis.type) {
            PayloadType.TEXT -> formatText(analysis)
            PayloadType.INTEGER -> formatNumber(analysis)
            PayloadType.DECIMAL -> formatNumber(analysis)
            PayloadType.BOOLEAN -> formatBoolean(analysis)
            PayloadType.URL -> formatUrl(analysis)
            PayloadType.EMAIL -> formatEmail(analysis)
            PayloadType.PHONE -> formatPhone(analysis)
            PayloadType.CURRENCY -> formatCurrency(analysis)
            PayloadType.PERCENTAGE -> formatPercentage(analysis)
            PayloadType.DATE_TIME -> formatDateTime(analysis)
            PayloadType.JSON_OBJECT -> formatJsonObject(analysis)
            PayloadType.JSON_ARRAY -> formatJsonArray(analysis)
            PayloadType.BASE64_IMAGE -> formatBase64Image(analysis)
            PayloadType.UNKNOWN -> formatUnknown(analysis)
        }

        return "$priorityPrefix$formattedContent$sourceInfo"
    }

    private fun formatText(analysis: PayloadAnalyzer.AnalysisResult): String {
        return "${PayloadType.TEXT.icon} **Text Message**\n\n${analysis.extractedValue}"
    }

    private fun formatNumber(analysis: PayloadAnalyzer.AnalysisResult): String {
        val typeLabel = if (analysis.type == PayloadType.INTEGER) "Integer Number" else "Decimal Number"
        return "${PayloadType.DECIMAL.icon} **$typeLabel**\n\n`${analysis.extractedValue}`"
    }

    private fun formatBoolean(analysis: PayloadAnalyzer.AnalysisResult): String {
        val icon = if (analysis.extractedValue.equals("TRUE", ignoreCase = true)) "✅" else "❌"
        return "$icon **Boolean Value**\n\n**Result:** ${analysis.extractedValue}"
    }

    private fun formatUrl(analysis: PayloadAnalyzer.AnalysisResult): String {
        return "${PayloadType.URL.icon} **URL Received**\n\n[${analysis.extractedValue}](${analysis.extractedValue})"
    }

    private fun formatEmail(analysis: PayloadAnalyzer.AnalysisResult): String {
        return "${PayloadType.EMAIL.icon} **Email Address**\n\n`${analysis.extractedValue}`"
    }

    private fun formatPhone(analysis: PayloadAnalyzer.AnalysisResult): String {
        return "${PayloadType.PHONE.icon} **Phone Number**\n\n`${analysis.extractedValue}`"
    }

    private fun formatCurrency(analysis: PayloadAnalyzer.AnalysisResult): String {
        return "${PayloadType.CURRENCY.icon} **Currency Amount**\n\n**Amount:** ${analysis.extractedValue}"
    }

    private fun formatPercentage(analysis: PayloadAnalyzer.AnalysisResult): String {
        return "${PayloadType.PERCENTAGE.icon} **Percentage**\n\n**Value:** ${analysis.extractedValue}"
    }

    private fun formatDateTime(analysis: PayloadAnalyzer.AnalysisResult): String {
        val parsedDate = try {
            val instant = Instant.parse(analysis.extractedValue)
            instant.toString().replace("T", " ").replace("Z", "")
        } catch (e: DateTimeParseException) {
            analysis.extractedValue
        }
        
        return "${PayloadType.DATE_TIME.icon} **Date/Time**\n\n**Parsed:** $parsedDate"
    }

    private fun formatJsonObject(analysis: PayloadAnalyzer.AnalysisResult): String {
        val keys = analysis.metadata["keys"] ?: "unknown"
        val preview = if (analysis.extractedValue.length > 500) {
            "${analysis.extractedValue.take(500)}..."
        } else {
            analysis.extractedValue
        }
        
        return "${PayloadType.JSON_OBJECT.icon} **JSON Object**\n\n**Keys:** $keys\n\n```json\n$preview\n```"
    }

    private fun formatJsonArray(analysis: PayloadAnalyzer.AnalysisResult): String {
        val size = analysis.metadata["size"] ?: "unknown"
        val preview = if (analysis.extractedValue.length > 500) {
            "${analysis.extractedValue.take(500)}..."
        } else {
            analysis.extractedValue
        }
        
        return "${PayloadType.JSON_ARRAY.icon} **JSON Array**\n\n**Items:** $size\n\n```json\n$preview\n```"
    }

    private fun formatBase64Image(analysis: PayloadAnalyzer.AnalysisResult): String {
        val mimeType = analysis.metadata["mimeType"] ?: "unknown"
        val size = analysis.metadata["size"] ?: "unknown"
        
        return "${PayloadType.BASE64_IMAGE.icon} **Image Received**\n\n**Type:** $mimeType\n**Size:** $size\n\n*Image data received and processed*"
    }

    private fun formatUnknown(analysis: PayloadAnalyzer.AnalysisResult): String {
        val preview = if (analysis.extractedValue.length > 200) {
            "${analysis.extractedValue.take(200)}..."
        } else {
            analysis.extractedValue
        }
        
        return "${PayloadType.UNKNOWN.icon} **Unknown Data Type**\n\n```\n$preview\n```"
    }
} 