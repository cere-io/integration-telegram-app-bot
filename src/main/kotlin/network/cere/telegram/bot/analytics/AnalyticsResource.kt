package network.cere.telegram.bot.analytics

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import kotlinx.serialization.Serializable
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.subscription.UserSubscription
import network.cere.telegram.bot.streaming.ton.ConnectedWallet
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestResponse
import java.time.LocalDateTime

@Path("analytics")
class AnalyticsResource {

    companion object {
        const val WHERE_CONDITION_PLACEHOLDER = "WHERE_CONDITION"
    }

    @GET
    @Path("subscriptions")
    @RunOnVirtualThread
    fun subscriptions(
        @RestQuery from: LocalDateTime,
        @RestQuery to: LocalDateTime,
        @RestQuery channelUsername: String?
    ): RestResponse<List<SubscriptionShort>> {
        val subscriptions = if (channelUsername.isNullOrEmpty())
            UserSubscription.find("subscribedAt between ?1 and ?2", from, to).list()
        else
            UserSubscription.find(
                "subscription.channel.username = ?1 and subscribedAt between ?2 and ?3",
                channelUsername,
                from,
                to
            ).list()

        return RestResponse.ok(subscriptions.map {
            SubscriptionShort(
                it.id.channelId,
                it.id.address,
                it.subscribedAt.toString()
            )
        })
    }

    @GET
    @Path("subscriptions/total")
    @RunOnVirtualThread
    fun subscriptionsTotal(@RestQuery channelUsername: String?): RestResponse<Total<Long>> {
        return if (channelUsername.isNullOrEmpty())
            RestResponse.ok(Total(UserSubscription.count()))
        else
            RestResponse.ok(Total(UserSubscription.count("subscription.channel.username = ?1", channelUsername)))
    }

    @GET
    @Path("connected_channels")
    @RunOnVirtualThread
    fun connectedChannels(
        @RestQuery from: LocalDateTime,
        @RestQuery to: LocalDateTime
    ): RestResponse<List<ChannelShort>> {
        return RestResponse.ok(
            Channel.find("connectedAt between ?1 and ?2", from, to).list()
                .map { ChannelShort(it.id, it.username, it.connectedAt.toString()) })
    }

    @GET
    @Path("connected_channels/total")
    @RunOnVirtualThread
    fun connectedChannelsTotal(): RestResponse<Total<Long>> = RestResponse.ok(Total(Channel.count()))

    @GET
    @Path("connected_channels/members")
    @RunOnVirtualThread
    fun connectedChannelsMembers(@RestQuery channelUsername: String?): RestResponse<List<Members>> {
        var query = """
            select ch.id, ch.memberCount, count(us.id.address) from Channel ch 
                left join Subscription s on ch.id = s.channel.id 
                left join UserSubscription us on s.id = us.subscription.id 
                $WHERE_CONDITION_PLACEHOLDER
            group by ch.id, ch.memberCount"""

        val whereCondition = if (!channelUsername.isNullOrEmpty()) { "where ch.username = '$channelUsername'" } else { "" }

        query = query.replace(WHERE_CONDITION_PLACEHOLDER, whereCondition)

        return RestResponse.ok(UserSubscription.find(query).project(Members::class.java).list())
    }

    @GET
    @Path("connected_wallets")
    @RunOnVirtualThread
    fun connectedWallets(
        @RestQuery from: LocalDateTime,
        @RestQuery to: LocalDateTime,
        @RestQuery channelUsername: String?
    ): RestResponse<List<WalletShort>> {
        val wallets = if (channelUsername.isNullOrEmpty()) {
            ConnectedWallet.find("connectedAt between ?1 and ?2", from, to).list()
        } else {
            val channel =
                Channel.find("username = ?1", channelUsername).firstResult() ?: return RestResponse.ok(emptyList())
            ConnectedWallet.find(
                "id.channelId = ?1 and connectedAt between ?2 and ?3",
                channel.id,
                from,
                to
            ).list()

        }
        return RestResponse.ok(wallets.map { WalletShort(it.id.channelId, it.id.address, it.connectedAt.toString()) })
    }

    @GET
    @Path("connected_wallets/total")
    @RunOnVirtualThread
    fun connectedWalletsTotal(@RestQuery channelUsername: String?): RestResponse<Total<Long>> {
        return if (channelUsername.isNullOrEmpty())
            RestResponse.ok(Total(ConnectedWallet.count()))
        else {
            val channel =
                Channel.find("username = ?1", channelUsername).firstResult() ?: return RestResponse.ok(Total(0))
            RestResponse.ok(Total(ConnectedWallet.count("id.channelId = ?1", channel.id)))
        }
    }

    @GET
    @Path("payments")
    @RunOnVirtualThread
    fun payments(
        @RestQuery from: LocalDateTime,
        @RestQuery to: LocalDateTime,
        @RestQuery channelUsername: String?
    ): RestResponse<List<Payment>> {
        val query =
            "select id.channelId, subscription.channel.username, id.address, subscribedAt, subscription.price from UserSubscription where subscribedAt between ?1 and ?2"
        val payments = if (channelUsername.isNullOrEmpty())
            UserSubscription.find(
                query,
                from,
                to
            ).project(Payment::class.java).list()
        else
            UserSubscription.find(
                "$query and subscription.channel.username = ?3",
                from,
                to,
                channelUsername
            ).project(Payment::class.java).list()

        return RestResponse.ok(payments)
    }

    @GET
    @Path("payments/total")
    @RunOnVirtualThread
    fun paymentsTotal(@RestQuery channelUsername: String?): RestResponse<Total<Double>> {
        val query = "select sum(subscription.price) from UserSubscription"
        return if (channelUsername.isNullOrEmpty())
            RestResponse.ok(Total(UserSubscription.find(query).project(Double::class.java).firstResult() ?: 0.0))
        else {
            RestResponse.ok(
                Total(
                    UserSubscription.find("$query where subscription.channel.username = ?1", channelUsername)
                        .project(Double::class.java).firstResult() ?: 0.0
                )
            )
        }
    }
}

@Serializable
data class ChannelShort(
    val id: Long,
    val username: String?,
    val connectedAt: String
)

@Serializable
data class SubscriptionShort(
    val channelId: Long,
    val address: String,
    val subscribedAt: String
)

@Serializable
data class WalletShort(
    val channelId: Long,
    val address: String,
    val connectedAt: String
)

@Serializable
data class Payment(
    val channelId: Long,
    val channelUsername: String?,
    val address: String,
    val paidAt: String,
    val amount: Float,
) {
    constructor(
        channelId: Long?,
        channelUsername: String?,
        address: String,
        paidAt: LocalDateTime,
        amount: Float?
    ) : this(channelId!!, channelUsername, address, paidAt.toString(), amount!!)
}

@Serializable
data class Total<T>(val total: T)

@Serializable
data class Members(
    val channelId: Long?,
    val total: Long?,
    val subscribed: Long?,
)
