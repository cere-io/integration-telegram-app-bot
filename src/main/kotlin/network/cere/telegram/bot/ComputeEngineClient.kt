package network.cere.telegram.bot

import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "compute-engine")
interface ComputeEngineClient {
    @POST
    @Path("/event/events")
    fun sendEvent(event: Event): Response
}