package network.cere.telegram.bot.streaming.campaign

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.transaction.Transactional
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.quest.Quest
import org.jboss.resteasy.reactive.RestHeader
import org.jboss.resteasy.reactive.RestResponse

@Path("campaigns")
class CampaignsResource {
    @GET
    @RunOnVirtualThread
    fun campaigns(@RestHeader xTelegramChat: Long): RestResponse<List<Campaign>> {
        val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()

        return RestResponse.ok(channel.campaigns)
    }

    @POST
    @RunOnVirtualThread
    @Transactional
    fun saveCampaign(@RestHeader xTelegramChat: Long, campaign: Campaign): RestResponse<String> {
        if (campaign.id == null) {
            val channel = Channel.findById(xTelegramChat) ?: return RestResponse.notFound()
            channel.addCampaign(campaign)
            channel.persistAndFlush()
        } else {
            val entity = Campaign.findById(campaign.id!!) ?: return RestResponse.notFound()
            entity.title = campaign.title
            entity.description = campaign.description

            if (campaign.quests.size > 0) {
                val quests = Quest.list("id in ?1", campaign.quests.map { it.id })

                entity.updateQuests(quests)
            }

            entity.persistAndFlush()
        }
        return RestResponse.ok()
    }

    @DELETE
    @Path("/{id}")
    @RunOnVirtualThread
    @Transactional
    fun deleteCampaign(@RestHeader xTelegramChat: Long, @PathParam("id") id: Long): RestResponse<Boolean> {
        return RestResponse.ok(Campaign.deleteById(id))
    }
}
