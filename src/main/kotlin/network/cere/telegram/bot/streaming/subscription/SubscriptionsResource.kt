package network.cere.telegram.bot.streaming.subscription

import io.quarkus.panache.common.Sort
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.Serializable
import network.cere.telegram.bot.streaming.ton.TonApi
import network.cere.telegram.bot.streaming.ton.TonConfig
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestResponse
import java.time.LocalDateTime

@Path("subscriptions")
class SubscriptionsResource(private val tonConfig: TonConfig, @RestClient private val tonApi: TonApi) {
    private companion object {
        private const val TON_VALUE_UNIT = 1000000000L
    }

    @GET
    @RunOnVirtualThread
    fun subscriptions(): SubscriptionsWithPayeeWallet {
        return SubscriptionsWithPayeeWallet(
            destinationWallet = tonConfig.wallet().nonbounceable(),
            subscriptions = Subscription.list()
        )
    }

    @GET
    @Path("{address}")
    @RunOnVirtualThread
    fun getSubscription(@RestPath address: String): RestResponse<Subscription> {
        val bounceableAddress = tonApi.detectAddress(address).result.bounceable.b64url
        return UserSubscription.findById(bounceableAddress)
            ?.subscription
            ?.let { RestResponse.ok(it) }
            ?: RestResponse.notFound()
    }

    @POST
    @Path("{address}")
    @RunOnVirtualThread
    @Transactional
    fun subscribe(@RestPath address: String): RestResponse<String> {
        val bounceableAddress = tonApi.detectAddress(address).result.bounceable.b64url
        if (UserSubscription.exists(bounceableAddress)) {
            return RestResponse.status(RestResponse.Status.CONFLICT, "Already subscribed")
        }
        val tx = tonApi.getTransactions(bounceableAddress).result
            .firstOrNull {
                it.inMsg.source == bounceableAddress && it.inMsg.destination == tonConfig.wallet().bounceable()
            }
            ?.inMsg
        if (tx != null) {
            val matchedSubscription = Subscription.listAll(Sort.by("price", Sort.Direction.Descending))
                .firstOrNull { TON_VALUE_UNIT * it.price <= tx.value }
            if (matchedSubscription != null) {
                UserSubscription(
                    address = bounceableAddress,
                    subscription = matchedSubscription,
                    expiresAt = LocalDateTime.now().plusDays(matchedSubscription.durationInDays.toLong())
                ).persistAndFlush()
                return RestResponse.ok()
            } else {
                return RestResponse.status(
                    RestResponse.Status.EXPECTATION_FAILED,
                    "Transaction value is below the lowest subscription price"
                )
            }
        } else {
            return RestResponse.notFound()
        }
    }
}

@Serializable
data class SubscriptionsWithPayeeWallet(
    val destinationWallet: String,
    val subscriptions: List<Subscription>
)