package network.cere.telegram.bot

import com.github.omarmiatello.telegram.TelegramRequest.GetFileRequest
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestPath

@Path("file")
class TelegramFileProxy(
    @RestClient private val botApi: BotApi,
    @RestClient private val botFileApi: BotFileApi,
) {
    @GET
    @Path("{fileId}")
    fun getFile(@RestPath fileId: String): java.nio.file.Path {
        return GetFileRequest(fileId)
            .let(botApi::getFile)
            .result
            ?.file_path
            ?.let(botFileApi::download) ?: throw RuntimeException("File not found")
    }
}