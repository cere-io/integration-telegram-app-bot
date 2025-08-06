package network.cere.telegram.bot

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "rule-service-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
interface RuleServiceClient {

    @POST
    @Path("/data-service/{dataServiceId}/query/get_avatar")
    fun getAvatar(
        @PathParam("dataServiceId") dataServiceId: String,
        request: AvatarRequestWrapper
    ): AvatarResponse
}