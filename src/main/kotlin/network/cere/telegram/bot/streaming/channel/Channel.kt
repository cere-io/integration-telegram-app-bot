package network.cere.telegram.bot.streaming.channel

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import network.cere.telegram.bot.streaming.subscription.Subscription
import network.cere.telegram.bot.streaming.video.Video
import java.time.LocalDateTime

@Entity
@Table(name = "connected_channels")
data class Channel(
    @Id
    val id: Long,

    val username: String?,

    val title: String,

    val connectedAt: LocalDateTime,

    val memberCount: Long,

    @Embedded
    val config: ChannelConfig = ChannelConfig(),

    @OneToMany(
        cascade = [CascadeType.ALL],
        mappedBy = "channel",
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val subscriptions: MutableList<Subscription> = mutableListOf(),

    @OneToMany(
        cascade = [CascadeType.ALL],
        mappedBy = "channel",
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val videos: MutableList<Video> = mutableListOf(),
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Channel, Long>

    fun addSubscription(subscription: Subscription): Channel {
        subscriptions += subscription
        subscription.channel = this
        return this
    }

    fun addVideo(video: Video): Channel {
        videos += video
        video.channel = this
        return this
    }

    fun isConfigured() = config.payoutAddress != null && config.botDdcAccessTokenBase58 != null && subscriptions.isNotEmpty()
}
