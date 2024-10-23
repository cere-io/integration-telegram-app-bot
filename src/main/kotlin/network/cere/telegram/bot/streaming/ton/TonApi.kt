package network.cere.telegram.bot.streaming.ton

import io.quarkus.cache.CacheResult
import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestQuery

@RegisterRestClient(configKey = "ton-api")
@ClientHeaderParam(name = "X-API-Key", value = ["\${ton.api.token}"])
interface TonApi {
    @GET
    @Path("getTransactions")
    @RunOnVirtualThread
    fun getTransactions(@RestQuery address: String, @RestQuery archival: Boolean = true): GetTransactionsResponse

    @GET
    @Path("detectAddress")
    @RunOnVirtualThread
    @CacheResult(cacheName = "ton-address-cache")
    fun detectAddress(@RestQuery address: String): DetectAddressResponse

    @GET
    @Path("getAddressBalance")
    @RunOnVirtualThread
    fun getAddressBalance(@RestQuery address: String): GetAddressBalanceResponse
}