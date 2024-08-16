package network.cere.telegram.bot.streaming.subscription

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.panache.common.Sort
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlinx.serialization.Serializable

@Entity
@Table(name = "subscriptions")
@Serializable
data class Subscription(
    @Id
    val id: Int,
    val durationInDays: Int,
    val description: String,
    val price: Float,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Subscription, Int> {
        fun list() = listAll(Sort.by("id", Sort.Direction.Ascending))
    }
}
