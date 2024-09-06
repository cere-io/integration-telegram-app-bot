package network.cere.telegram.bot.streaming.ton

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestPath

@Path("wallets")
class WalletResource(@RestClient private val tonApi: TonApi) {
    @GET
    @Path("{address}/balance")
    fun getAddressBalance(@RestPath address: String) = tonApi.getAddressBalance(address).result
}