package network.cere.telegram.bot.streaming.video

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlinx.serialization.Serializable

@Entity
@Table(name = "videos")
@Serializable
data class Video(
    @Id
    val url: String,
    val name: String,
    val description: String,
    val width: Long,
    val height: Long,
    val duration: Long,
    val fileSize: Long,
    val thumbnailUrl: String?,
    val mimeType: String?,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Video, String>
}