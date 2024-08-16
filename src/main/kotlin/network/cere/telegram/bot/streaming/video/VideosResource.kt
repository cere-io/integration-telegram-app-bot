package network.cere.telegram.bot.streaming.video

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path

@Path("videos")
class VideosResource {
    @GET
    @RunOnVirtualThread
    fun videos(): List<Video> {
        return Video.listAll()
    }
}