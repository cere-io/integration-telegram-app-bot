package network.cere.telegram.bot.streaming.subscription

import io.quarkus.panache.common.Sort
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.Serializable
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.ton.TonApi
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestResponse
import java.time.LocalDateTime

@Path("subscriptions")
class SubscriptionsResource(@RestClient private val tonApi: TonApi) {
    private companion object {
        private const val TON_VALUE_UNIT = 1000000000L
    }

    @GET
    @RunOnVirtualThread
    fun subscriptions(@RestHeader xTelegramChat: Long): RestResponse<SubscriptionsWithPayeeWallet> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        if (!channel.isConfigured()) return RestResponse.notFound()
        return RestResponse.ok(
            SubscriptionsWithPayeeWallet(
                destinationWallet = requireNotNull(channel.config.payoutAddress),
                subscriptions = channel.subscriptions
            )
        )
    }

    @GET
    @Path("{address}")
    @RunOnVirtualThread
    fun getSubscription(@RestHeader xTelegramChat: Long, @RestPath address: String): RestResponse<Subscription> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        if (!channel.isConfigured()) return RestResponse.notFound()
        val bounceableAddress = tonApi.detectAddress(address).result.bounceable.b64url
        return UserSubscription.findById(bounceableAddress, channel.id)
            ?.subscription
            ?.let { RestResponse.ok(it) }
            ?: RestResponse.notFound()
    }

    @POST
    @Path("{address}")
    @RunOnVirtualThread
    @Transactional
    fun subscribe(@RestHeader xTelegramChat: Long, @RestPath address: String): RestResponse<String> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        if (!channel.isConfigured()) return RestResponse.notFound()
        val bounceableAddress = tonApi.detectAddress(address).result.bounceable.b64url
        if (UserSubscription.exists(bounceableAddress, channel.id)) {
            return RestResponse.status(RestResponse.Status.CONFLICT, "Already subscribed")
        }
        var txFound = false
        var retriesLeft = 3
        while (retriesLeft > 0 && !txFound) {
            val tx = tonApi.getTransactions(bounceableAddress).result
                .flatMap { it.outMsgs }
                .filter { it.source == bounceableAddress && it.destination == requireNotNull(channel.config.payoutAddress) }
                .maxByOrNull { it.value }
            if (tx != null) {
                val matchedSubscription = Subscription.listAll(Sort.by("price", Sort.Direction.Descending))
                    .firstOrNull { TON_VALUE_UNIT * it.price <= tx.value }
                if (matchedSubscription != null) {
                    UserSubscription(
                        id = UserSubscriptionKey(bounceableAddress, channel.id),
                        subscription = matchedSubscription,
                        expiresAt = LocalDateTime.now().plusDays(matchedSubscription.durationInDays.toLong())
                    ).persistAndFlush()
                    txFound = true
                }
            }
            if (!txFound) {
                Thread.sleep(3000)
            }
            retriesLeft--
        }
        return if (txFound) RestResponse.ok() else RestResponse.notFound()
    }
}

@Serializable
data class SubscriptionsWithPayeeWallet(
    val destinationWallet: String,
    val subscriptions: List<Subscription>
)