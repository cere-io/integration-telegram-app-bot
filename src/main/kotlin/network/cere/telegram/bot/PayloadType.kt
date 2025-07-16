package network.cere.telegram.bot

import kotlinx.serialization.Serializable

@Serializable
enum class PayloadType(val description: String, val icon: String) {
    TEXT("Simple text message", "📝"),
    INTEGER("Whole number", "🔢"),
    DECIMAL("Decimal number", "🔢"),
    BOOLEAN("Boolean value", "✅"),
    URL("Web URL", "🔗"),
    EMAIL("Email address", "📧"),
    PHONE("Phone number", "📞"),
    CURRENCY("Currency amount", "💰"),
    PERCENTAGE("Percentage value", "📊"),
    DATE_TIME("Date/Time string", "📅"),
    JSON_OBJECT("JSON object", "🏗️"),
    JSON_ARRAY("JSON array", "📋"),
    BASE64_IMAGE("Base64 encoded image", "🖼️"),
    UNKNOWN("Unknown data type", "❓");

    companion object {
        fun getAllTypes(): Map<String, String> {
            return values().associate { it.name to it.description }
        }
    }
} 