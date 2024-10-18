package network.cere.telegram.bot.streaming.subscription

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

@Embeddable
data class UserSubscriptionKey(
    @Column
    val address: String,

    @Column
    val channelId: Long,
) : Serializable
