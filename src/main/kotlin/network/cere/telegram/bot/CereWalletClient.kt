package network.cere.telegram.bot

import io.quarkus.cache.CacheResult
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestQuery

@RegisterRestClient(configKey = "cere-wallet")
interface CereWalletClient {
    @GET
    @Path("auth/s2s/wallet-by-telegram-user-id")
    @ClientHeaderParam(name = "Authorization", value = ["\${cere-wallet.token}"])
    @CacheResult(cacheName = "wallet-cache")
    fun walletByTelegramUserId(@RestQuery telegramUserId: Long): CereWalletResponse
}