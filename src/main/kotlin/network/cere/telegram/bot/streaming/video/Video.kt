package network.cere.telegram.bot.streaming.video

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import network.cere.telegram.bot.streaming.channel.Channel

@Entity
@Table(name = "videos")
@Serializable
data class Video(
    @Transient
    @ManyToOne(fetch = FetchType.LAZY)
    var channel: Channel? = null,
    val url: String,
    var title: String = "",
    var description: String = "",
    var thumbnailUrl: String? = null,
) : PanacheEntity() {
    companion object : PanacheCompanion<Video>
}