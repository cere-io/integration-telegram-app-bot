package network.cere.telegram.bot

import com.github.omarmiatello.telegram.Update
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class GroupMessageHandler(
    @RestClient private val computeEngineClient: ComputeEngineClient,
    private val config: Config,
    private val signer: Signer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val groupConfigs = config.groups().entries.associate { it.value.groupId() to it.value }

    fun handle(update: Update) {
        val message = update.message ?: return
        val chat = message.chat
        val groupId = chat.id.longValue
        val groupConfig = groupConfigs[groupId]
        if (groupConfig == null) {
            log.warn("Group {} with id {} not configured for message onboarding", chat.title, groupId)
            return
        }
        val from = message.from
        if (from == null) {
            log.warn("Unable to identify message author")
            return
        }
        val event = Event(
            payload = EventPayload(
                orgId = groupConfig.orgId(),
                campaignId = groupConfig.campaignId(),
                groupId = groupId,
                messageId = message.message_id.longValue,
                dateUnixTime = message.date,
                fromUserId = from.id.longValue,
                fromUserName = from.username ?: "unknown",
                text = message.text
                    ?: message.caption
                    ?: when {
                        message.photo != null -> "[Photo]"
                        message.video != null -> "[Video]"
                        message.document != null -> "[Document]"
                        message.sticker != null -> "[Sticker]"
                        else -> "[Unsupported message type]"
                    },
            ),
            appId = config.appId(),
            accountId = "", // TODO
            userPubKey = "", //TODO
            dataServicePubKey = signer.publicKey,
        ).sign(signer)

        runCatching {
            computeEngineClient.sendEvent(event)
            log.info("✅ Event sent successfully")
        }.onFailure {
            log.error("❌ Failed to send event", it)
        }
    }
}
