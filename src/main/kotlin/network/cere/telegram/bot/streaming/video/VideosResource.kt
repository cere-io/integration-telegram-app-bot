package network.cere.telegram.bot.streaming.video

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import network.cere.telegram.bot.streaming.channel.Channel
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse

@Path("videos")
class VideosResource {
    @GET
    @RunOnVirtualThread
    fun videos(@RestHeader xTelegramChat: Long): RestResponse<List<Video>> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        if (!channel.isConfigured()) return RestResponse.notFound()

        return RestResponse.ok(channel.videos)
    }
}