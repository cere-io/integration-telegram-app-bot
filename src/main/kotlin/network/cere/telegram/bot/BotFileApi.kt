package network.cere.telegram.bot

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestPath

@RegisterRestClient(configKey = "tg-file-api")
interface BotFileApi {
    @GET
    @Path("{filePath}")
    fun download(@RestPath filePath: String): java.nio.file.Path
}