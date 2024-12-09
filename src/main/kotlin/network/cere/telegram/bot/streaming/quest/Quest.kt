package network.cere.telegram.bot.streaming.quest

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
@Table(name = "quests")
@Serializable
data class Quest(
    @Id
    @GeneratedValue
    var id: Long? = null,
    var title: String,
    var description: String,
    var type: String,
    var videoId: String = "",
    var xUrl: String = "",
    var rewardPoints: Long,

    @Transient
    @ManyToOne(fetch = FetchType.LAZY)
    var channel: Channel? = null,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Quest, Long>
}
