package network.cere.telegram.bot.streaming.quest

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

@Path("quests")
class QuestsResource {
    @GET
    @RunOnVirtualThread
    fun quests(@RestHeader xTelegramChat: Long): RestResponse<List<Quest>> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()

        return RestResponse.ok(channel.quests)
    }

    @POST
    @RunOnVirtualThread
    @Transactional
    fun saveQuest(@RestHeader xTelegramChat: Long, quest: Quest): RestResponse<Quest> {
        if (quest.id == null) {
            val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
            channel.addQuest(quest)
            channel.persistAndFlush()
            return RestResponse.ok(quest)
        } else {
            val entity = Quest.findById(quest.id!!) ?: return RestResponse.notFound()
            entity.title = quest.title
            entity.description = quest.description
            entity.type = quest.type
            entity.videoId = quest.videoId
            entity.rewardPoints = quest.rewardPoints
            entity.persistAndFlush()
            return RestResponse.ok(entity)
        }
    }

    @DELETE
    @Path("/{id}")
    @RunOnVirtualThread
    @Transactional
    fun deleteQuest(@RestHeader xTelegramChat: Long, @PathParam("id") id: Long): RestResponse<Boolean> {
        return RestResponse.ok(Quest.deleteById(id))
    }
}
