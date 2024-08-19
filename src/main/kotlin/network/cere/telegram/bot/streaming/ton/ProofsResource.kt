package network.cere.telegram.bot.streaming.ton

import dev.sublab.base58.base58
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import network.cere.telegram.bot.streaming.ddc.Wallet
import network.cere.telegram.bot.streaming.subscription.UserSubscription
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestResponse
import java.time.LocalDateTime
import java.util.*
import kotlin.random.asKotlinRandom

@Path("proofs")
class ProofsResource(
    @RestClient private val tonApi: TonApi,
    private val random: Random,
    private val wallet: Wallet,
) {
    @GET
    @RunOnVirtualThread
    @Transactional
    fun generatePayload(): String {
        val payload = random.asKotlinRandom().nextBytes(32).base58.encode()
        ProofPayload(payload).persistAndFlush()
        return payload
    }

    @POST
    @RunOnVirtualThread
    fun issueDdcAuthToken(rq: CheckProofRequest): RestResponse<String> {
        //TODO verify rq properly

        val address = tonApi.detectAddress(rq.address).result
        val bounceableAddress = address.bounceable.b64url
        val subscription = UserSubscription.findById(bounceableAddress)
        return if (subscription == null || subscription.expiresAt.isBefore(LocalDateTime.now())) {
            RestResponse.ok("")
        } else {
            RestResponse.ok(wallet.grantAccess())
        }
    }
}