package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.TelegramRequest
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.Json

@Path("telegram")
class TelegramApiClient(
    @RestClient private val botApi: BotApi,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @POST
    @Path("/get-channel")
    fun getChannel(request: TelegramChannelSearchRequest): Response {
        val results = mutableListOf<TelegramChannel>()

        try {
            val channelName = request.channelName.trim().removePrefix("@")
            val chatId = "@$channelName"

            try {
                val jsonResponse = botApi.getChat(TelegramRequest.GetChatRequest(chat_id = ChatId(chatId)))

                val json = Json { ignoreUnknownKeys = true }
                val telegramResponse = json.decodeFromString<TelegramApiResponse>(jsonResponse)
                
                val chat = telegramResponse.result

                if (chat != null && isChannel(chat)) {
                    results.add(chat.toTelegramChannel())
                    log.info("Found channel: ${chat.title} (@${chat.channelName})")
                }
            } catch (e: Exception) {
                log.debug("Channel not found by username: $channelName", e)
                log.error("Error parsing response: ${e.message}", e)
            }
            
            return Response.ok(
                TelegramChannelSearchResponse(
                    success = true,
                    channels = results
                )
            ).build()

        } catch (e: Exception) {
            log.error("Error searching for channel: ${request.channelName}", e)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    TelegramChannelSearchResponse(
                        success = false,
                        error = "Failed to search channel: ${e.message}"
                    )
                )
                .build()
        }
    }

    private fun isChannel(chat: TelegramChat): Boolean {
        return chat.type == "channel" || chat.type == "supergroup"
    }

    private fun TelegramChat.toTelegramChannel(): TelegramChannel {
        return TelegramChannel(
            id = this.id,
            title = this.title ?: "Unknown Channel",
            name = this.channelName,
            type = this.type,
            memberCount = null
        )
    }
}