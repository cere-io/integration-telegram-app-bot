package network.cere.telegram.bot

import io.quarkus.cache.CacheResult
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestQuery

@RegisterRestClient(configKey = "rob-api")
interface RobClient {
    @GET
    @Path("/campaign")
    @CacheResult(cacheName = "campaigns-cache")
    fun getCampaigns(
        @RestQuery("dataServiceId") dataServiceId: String,
        @RestQuery("organizationId") organizationId: String? = null,
    ): RobApiResponse<List<Campaign>>

    @GET
    @Path("/campaign/{campaignId}")
    @CacheResult(cacheName = "campaign-cache")
    fun getCampaign(@PathParam("campaignId") campaignId: String): RobApiResponse<Campaign>
} 