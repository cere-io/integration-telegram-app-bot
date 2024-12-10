package network.cere.telegram.bot.streaming.user

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanionBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "bot_users")
data class BotUser(
    @Id
    val id: Long,
    val isBot: Boolean,
    val firstName: String,
    var chatContextJson: String,
    var channels: String?,
) : PanacheEntityBase {
    companion object : PanacheCompanionBase<BotUser, Long>
}
