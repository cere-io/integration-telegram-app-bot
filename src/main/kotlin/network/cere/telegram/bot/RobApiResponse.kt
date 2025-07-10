package network.cere.telegram.bot

import kotlinx.serialization.Serializable

@Serializable
data class RobApiResponse<T>(
    val code: String,
    val data: T,
    val message: String? = null,
    val details: String? = null,
) 