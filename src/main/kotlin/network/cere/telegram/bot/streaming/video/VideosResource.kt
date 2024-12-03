package network.cere.telegram.bot.streaming.video

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import network.cere.telegram.bot.streaming.channel.Channel
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse

@Path("videos")
class VideosResource {
    @GET
    @RunOnVirtualThread
    fun videos(@RestHeader xTelegramChat: Long): RestResponse<List<Video>> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
        return RestResponse.ok(channel.videos)
    }

    @POST
    @RunOnVirtualThread
    @Transactional
    fun saveVideo(@RestHeader xTelegramChat: Long, video: Video): RestResponse<Video> {
        if (video.id == null) {
            val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
            channel.addVideo(video)
            channel.persistAndFlush()
            return RestResponse.ok(video)
        } else {
            val entity = Video.findById(video.id!!) ?: return RestResponse.notFound()
            entity.title = video.title
            entity.description = video.description
            entity.thumbnailUrl = video.thumbnailUrl
            entity.url = video.url
            entity.persistAndFlush()
            return RestResponse.ok(entity)
        }
    }

    @DELETE
    @Path("/{id}")
    @RunOnVirtualThread
    @Transactional
    fun deleteVideo(@RestHeader xTelegramChat: Long, @PathParam("id") id: Long): RestResponse<Boolean> {
        return RestResponse.ok(Video.deleteById(id))
    }
}
