package network.cere.telegram.bot

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

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
    val caption: String,
    @Serializable(with = InstantSerializer::class)
    val last_boost_at: Instant = Instant.now()
)

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}