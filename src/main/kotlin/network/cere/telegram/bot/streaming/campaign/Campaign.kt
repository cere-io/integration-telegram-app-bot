package network.cere.telegram.bot.streaming.campaign

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.quest.Quest
import kotlinx.serialization.Contextual
import java.util.Date

@Entity
@Table(name = "campaigns")
@Serializable
data class Campaign(
    @Id
    @GeneratedValue
    var id: Long? = null,
    var title: String,
    var description: String,

    @Contextual
    @Temporal(TemporalType.TIMESTAMP)
    var start_date: Date? = null,

    @Contextual
    @Temporal(TemporalType.TIMESTAMP)
    var end_date: Date? = null,

    @Transient
    @ManyToOne(fetch = FetchType.LAZY)
    var channel: Channel? = null,

    @ManyToMany(
        cascade = [CascadeType.ALL],
        fetch = FetchType.EAGER
    )
    @JoinTable(
        name = "campaigns_quests",
        joinColumns = [JoinColumn(name = "campaign_id")],
        inverseJoinColumns = [JoinColumn(name = "quest_id")])
    val quests: MutableList<Quest> = mutableListOf(),
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<Campaign, Long>

    fun updateQuests(quests: List<Quest>): Campaign {
        this.quests.clear()
        this.quests.addAll(quests)
        return this
    }
}
