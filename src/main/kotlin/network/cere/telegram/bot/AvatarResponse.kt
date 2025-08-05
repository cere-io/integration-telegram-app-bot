package network.cere.telegram.bot

import kotlinx.serialization.Serializable

@Serializable
data class AvatarResponse(
    val result: AvatarResult
)

@Serializable
data class AvatarResult(
    val code: String,
    val data: AvatarData? = null
)

@Serializable
data class AvatarData(
    val success: Boolean,
    val data: AvatarInfo? = null,
    val emittedEvents: List<String>
)

@Serializable
data class AvatarInfo(
    val level: Int,
    val url: String,
    val caption: String
)