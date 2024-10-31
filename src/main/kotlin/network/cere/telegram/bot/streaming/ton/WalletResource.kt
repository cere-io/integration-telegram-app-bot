package network.cere.telegram.bot.streaming.ton

import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestPath
import java.time.LocalDateTime

@Path("wallets")
class WalletResource(@RestClient private val tonApi: TonApi) {
    @GET
    @Path("{address}/balance")
    @Transactional
    fun getAddressBalance(@RestHeader xTelegramChat: Long, @RestPath address: String): String {
        if (ConnectedWallet.findById(ConnectedWalletKey(address, xTelegramChat)) == null) {
            ConnectedWallet(ConnectedWalletKey(address, xTelegramChat), LocalDateTime.now()).persistAndFlush()
        }
        return tonApi.getAddressBalance(address).result
    }
}
