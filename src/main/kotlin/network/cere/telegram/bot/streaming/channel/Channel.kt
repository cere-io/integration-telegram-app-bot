package network.cere.telegram.bot.streaming.channel

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.*
import network.cere.telegram.bot.streaming.subscription.Subscription
import network.cere.telegram.bot.streaming.video.Video

@Entity
@Table(name = "connected_channels")
data class Channel(
    @Id
    val id: Long,

    @Embedded
    val config: ChannelConfig = ChannelConfig(),

    val title: String,

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

    fun isConfigured() = config.payoutAddress != null && config.botDdcAccessTokenBase58 != null
}