package network.cere.telegram.bot.streaming.ton

import dev.sublab.base58.base58
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.ddc.Wallet
import network.cere.telegram.bot.streaming.subscription.UserSubscription
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse
import java.time.LocalDateTime
import java.util.*
import kotlin.random.asKotlinRandom

@Path("proofs")
class ProofsResource(
    @RestClient private val tonApi: TonApi,
    @Suppress("CdiInjectionPointsInspection") private val random: Random,
    private val wallet: Wallet,
) {
    @GET
    @RunOnVirtualThread
    @Transactional
    fun generatePayload(@RestHeader xTelegramChat: Long): RestResponse<String> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        if (!channel.isConfigured()) return RestResponse.notFound()
        val payload = random.asKotlinRandom().nextBytes(32).base58.encode()
        ProofPayload(payload).persistAndFlush()
        return RestResponse.ok(payload)
    }

    @POST
    @RunOnVirtualThread
    fun issueDdcAuthToken(@RestHeader xTelegramChat: Long, rq: CheckProofRequest): RestResponse<String> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        if (!channel.isConfigured()) return RestResponse.notFound()

        //TODO verify rq properly

        val address = tonApi.detectAddress(rq.address).result
        val bounceableAddress = address.bounceable.b64url
        val subscription = UserSubscription.findById(bounceableAddress, channel.id)
        return when {
            subscription == null -> RestResponse.ok("")
            subscription.expiresAt.isBefore(LocalDateTime.now()) -> {
                subscription.delete()
                RestResponse.ok("")
            }

            else -> RestResponse.ok(wallet.grantAccess(requireNotNull(channel.config.botDdcAccessTokenBase58)))
        }
    }
}
