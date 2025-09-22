package network.cere.telegram.bot

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.MessageId
import com.github.omarmiatello.telegram.ReplyParameters
import com.github.omarmiatello.telegram.TelegramRequest
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Base64

@Path("meme")
class MemeCallback(
    @RestClient private val computeEngineClient: ComputeEngineClient,
    @RestClient private val botApi: BotApi,
    @RestClient private val cereWalletClient: CereWalletClient,
    @ConfigProperty(name = "ddc.cdnUrl") cdnUrl: String,
    @ConfigProperty(name = "ddc.bucket") bucket: String,
    private val json: Json,
    private val config: Config,
    private val signer: Signer,
    private val ddcService: DdcService,
    ) {
    private companion object {
        private const val USER_AVATAR_GENERATED = "USER_AVATAR_GENERATED"
    }
    private val log = LoggerFactory.getLogger(javaClass)

    private val ddcFileUrl = "${cdnUrl}/${bucket}/"

    @POST
    fun replyWithMeme(rq: MemeCallbackRequest): Unit {
        try {
            val chatId = ChatId(rq.groupId.toString())
            val messageId = rq.messageId
            val orgId = rq.orgId
            val campaignId = rq.campaignId
            val prompt = rq.prompt
            val userName = rq.userName
            val userId = rq.userId
            val imageUrl = rq.imageUrl
            val imageCid = rq.imageCid
            val imageBase64 = rq.imageBase64
            val boost = rq.boost
            when {
                rq.imageUrl != null     -> sendPhoto(chatId.longValue, rq.messageId, imageUrl!!)
                rq.imageCid != null     -> sendPhoto(chatId.longValue, rq.messageId, "$ddcFileUrl${imageCid}")
                rq.imageBase64 != null  -> {
                    val imageBytes = Base64.getDecoder().decode(imageBase64)

                    val cid = ddcService.storeFile(imageBytes)

                    val imageUrl = "$ddcFileUrl$cid"

                    sendPhoto(chatId.longValue, rq.messageId, imageUrl)

                    val wallet = cereWalletClient.walletByTelegramUserId(userId).data

                    val event = Event(
                        payload = MemeCallbackRequest(
                            userId,
                            userName,
                            orgId,
                            campaignId,
                            groupId = chatId.longValue,
                            messageId,
                            imageUrl = "$ddcFileUrl${cid}",
                            imageCid = cid,
                            imageBase64 = null,
                            prompt,
                            boost = boost ?: false,
                        ).let(json::encodeToJsonElement),
                        appId = config.appId(),
                        accountId = wallet.accountId,
                        userPubKey = wallet.userPubKey,
                        dataServicePubKey = signer.publicKey,
                        signing = byteArrayOf(0x00, 0x01, 0x00).hex(false),
                        type = USER_AVATAR_GENERATED
                    ).sign(signer)
                    runCatching {
                        computeEngineClient.sendEvent(event)
                        log.info("✅ Event ${USER_AVATAR_GENERATED} sent successfully")
                    }.onFailure {
                        log.error("❌ Failed to send event", it)
                    }
                }
                else -> error("No image provided")
            }
        } catch (e: Exception) {
            log.error("Failed to send avatar", e)
        }
        return Unit
    }

    private fun sendPhoto(groupId: Long, messageId: Long, url: String) {
        botApi.sendPhoto(
            TelegramRequest.SendPhotoRequest(
                chat_id = ChatId(groupId.toString()),
                photo = url,
                caption = "Here you go! This is the result \uD83D\uDD25",
                reply_parameters = ReplyParameters(
                    chat_id = ChatId(groupId.toString()),
                    message_id = MessageId(messageId)
                )
            )
        )
        log.info("✅ Meme sent: $url")
    }
}