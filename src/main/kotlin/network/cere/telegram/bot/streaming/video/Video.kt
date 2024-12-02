package network.cere.telegram.bot.streaming.video

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import network.cere.telegram.bot.streaming.channel.Channel

@Entity
@Table(name = "videos")
@Serializable
data class Video(
    @Id
    @GeneratedValue
    var id: Long? = null,
    @Transient
    @ManyToOne(fetch = FetchType.LAZY)
    var channel: Channel? = null,
    var url: String,
    var title: String = "",
    var description: String = "",
    var thumbnailUrl: String? = null,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Video, Long>
}
