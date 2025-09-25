package network.cere.telegram.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    @SerialName("id")
    val id: String,

    @SerialName("dataServiceId")
    val dataServiceId: String,

    @SerialName("name")
    val name: String,

    @SerialName("archived")
    val archived: Boolean,

    @SerialName("createdAt")
    val createdAt: String,

    @SerialName("updatedAt")
    val updatedAt: String?
)