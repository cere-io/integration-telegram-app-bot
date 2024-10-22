package network.cere.telegram.bot.streaming.subscription

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
@Table(name = "subscriptions")
@Serializable
data class Subscription(
    @Transient
    @ManyToOne(fetch = FetchType.LAZY)
    var channel: Channel? = null,
    val durationInDays: Int,
    var description: String,
    var price: Float,
) : PanacheEntity() {
    companion object : PanacheCompanion<Subscription>
}
