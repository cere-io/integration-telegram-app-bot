package network.cere.telegram.bot.streaming.subscription

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_subscriptions")
data class UserSubscription(
    @Id
    val address: String,

    @OneToOne(targetEntity = Subscription::class, optional = false)
    val subscription: Subscription,

    val expiresAt: LocalDateTime, //TODO clean up old subscriptions
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<UserSubscription, String> {
        fun exists(address: String) = findById(address) != null
    }
}
