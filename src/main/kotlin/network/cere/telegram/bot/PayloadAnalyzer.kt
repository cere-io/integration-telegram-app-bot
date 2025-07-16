package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

@ApplicationScoped
class PayloadAnalyzer {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        // Regex patterns for different data types
        private val URL_PATTERN = Pattern.compile(
            "^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE
        )
        private val EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        )
        private val PHONE_PATTERN = Pattern.compile(
            "^[+]?[1-9]?[0-9]{7,15}$|^[+]?[1-9]?[-\\s().0-9]{10,20}$"
        )
        private val CURRENCY_PATTERN = Pattern.compile(
            "^[\\$€£¥]?\\s?[0-9]{1,3}(?:[,.]?[0-9]{3})*(?:[.,][0-9]{2})?\\s?[\\$€£¥]?$"
        )
        private val PERCENTAGE_PATTERN = Pattern.compile(
            "^[0-9]+(?:[.,][0-9]+)?\\s?%$"
        )
        private val DATE_TIME_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?(?:Z|[+-]\\d{2}:\\d{2})?$|^\\d{4}-\\d{2}-\\d{2}$"
        )
        private val BASE64_IMAGE_PATTERN = Pattern.compile(
            "^data:image/[a-zA-Z]+;base64,[A-Za-z0-9+/]+=*$"
        )
    }

    data class AnalysisResult(
        val type: PayloadType,
        val originalValue: JsonElement,
        val extractedValue: String,
        val metadata: Map<String, String> = emptyMap()
    )

    fun analyze(payload: JsonElement): AnalysisResult {
        log.debug("Analyzing payload: {}", payload)
        
        val result = when (payload) {
            is JsonPrimitive -> analyzePrimitive(payload)
            is JsonObject -> AnalysisResult(
                type = PayloadType.JSON_OBJECT,
                originalValue = payload,
                extractedValue = payload.toString(),
                metadata = mapOf("keys" to payload.keys.joinToString(", "))
            )
            is JsonArray -> AnalysisResult(
                type = PayloadType.JSON_ARRAY,
                originalValue = payload,
                extractedValue = payload.toString(),
                metadata = mapOf("size" to payload.size.toString())
            )
            else -> AnalysisResult(
                type = PayloadType.UNKNOWN,
                originalValue = payload,
                extractedValue = payload.toString()
            )
        }
        
        log.debug("Analysis result: type={}, extractedValue={}", result.type, result.extractedValue)
        return result
    }

    private fun analyzePrimitive(primitive: JsonPrimitive): AnalysisResult {
        return when {
            primitive.isString -> analyzeString(primitive.content)
            primitive.booleanOrNull != null -> AnalysisResult(
                type = PayloadType.BOOLEAN,
                originalValue = primitive,
                extractedValue = primitive.boolean.toString().uppercase()
            )
            primitive.intOrNull != null -> AnalysisResult(
                type = PayloadType.INTEGER,
                originalValue = primitive,
                extractedValue = primitive.int.toString()
            )
            primitive.doubleOrNull != null -> AnalysisResult(
                type = PayloadType.DECIMAL,
                originalValue = primitive,
                extractedValue = primitive.double.toString()
            )
            else -> AnalysisResult(
                type = PayloadType.UNKNOWN,
                originalValue = primitive,
                extractedValue = primitive.toString()
            )
        }
    }

    private fun analyzeString(content: String): AnalysisResult {
        val trimmed = content.trim()
        
        return when {
            BASE64_IMAGE_PATTERN.matcher(trimmed).matches() -> {
                val parts = trimmed.split(";base64,")
                val mimeType = parts[0].substringAfter("data:image/")
                AnalysisResult(
                    type = PayloadType.BASE64_IMAGE,
                    originalValue = JsonPrimitive(content),
                    extractedValue = trimmed,
                    metadata = mapOf(
                        "mimeType" to mimeType,
                        "size" to "${parts.getOrNull(1)?.length ?: 0} characters"
                    )
                )
            }
            URL_PATTERN.matcher(trimmed).matches() -> AnalysisResult(
                type = PayloadType.URL,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
            EMAIL_PATTERN.matcher(trimmed).matches() -> AnalysisResult(
                type = PayloadType.EMAIL,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
            PHONE_PATTERN.matcher(trimmed.replace("\\s|-|\\(|\\)|\\.".toRegex(), "")).matches() -> AnalysisResult(
                type = PayloadType.PHONE,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
            CURRENCY_PATTERN.matcher(trimmed).matches() -> AnalysisResult(
                type = PayloadType.CURRENCY,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
            PERCENTAGE_PATTERN.matcher(trimmed).matches() -> AnalysisResult(
                type = PayloadType.PERCENTAGE,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
            DATE_TIME_PATTERN.matcher(trimmed).matches() -> AnalysisResult(
                type = PayloadType.DATE_TIME,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
            else -> AnalysisResult(
                type = PayloadType.TEXT,
                originalValue = JsonPrimitive(content),
                extractedValue = trimmed
            )
        }
    }
} 