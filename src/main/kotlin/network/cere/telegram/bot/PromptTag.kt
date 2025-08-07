package network.cere.telegram.bot

import kotlinx.serialization.Serializable

@Serializable
data class PromptTag(
    val tag: String,
    val prompt: String
)