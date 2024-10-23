package network.cere.telegram.bot.streaming.subscription

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_subscriptions")
data class UserSubscription(
    @EmbeddedId
    val id: UserSubscriptionKey,

    @OneToOne(targetEntity = Subscription::class, optional = false)
    val subscription: Subscription,

    val expiresAt: LocalDateTime,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<UserSubscription, UserSubscriptionKey> {
        fun exists(address: String, channelId: Long) = findById(UserSubscriptionKey(address, channelId)) != null

        fun findById(address: String, channelId: Long) = findById(UserSubscriptionKey(address, channelId))
    }
}
