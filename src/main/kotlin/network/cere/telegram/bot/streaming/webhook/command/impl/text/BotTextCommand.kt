package network.cere.telegram.bot.streaming.webhook.command.impl.text

import com.github.omarmiatello.telegram.ChatId
import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.TelegramRequest
import com.github.omarmiatello.telegram.Update
import com.google.common.net.UrlEscapers
import dev.sublab.base58.base58
import dev.sublab.hex.hex
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import network.cere.ddc.AuthToken
import network.cere.telegram.bot.api.BotApi
import network.cere.telegram.bot.streaming.channel.Channel
import network.cere.telegram.bot.streaming.ddc.Wallet
import network.cere.telegram.bot.streaming.subscription.Subscription
import network.cere.telegram.bot.streaming.ton.TonApi
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.user.ContextEntity
import network.cere.telegram.bot.streaming.user.ContextModificationStep
import network.cere.telegram.bot.streaming.video.Video
import network.cere.telegram.bot.streaming.webhook.BotProducer
import network.cere.telegram.bot.streaming.webhook.replyKeyboardMarkup
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

@ApplicationScoped
class BotTextCommand(
    private val json: Json,
    private val botProducer: BotProducer,
    private val wallet: Wallet,
    @RestClient private val tonApi: TonApi,
    @RestClient private val botApi: BotApi,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun tryHandle(update: Update) {
        if (update.message?.text == null) return

        val message = requireNotNull(update.message)
        val from = requireNotNull(message.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)

        when (chatContext.entityName) {
            ContextEntity.SUBSCRIPTION -> handleAddSubscription(message, user, chatContext)
            ContextEntity.TOKEN -> handleSetToken(message, user, chatContext)
            ContextEntity.PAYOUT_ADDRESS -> handleSetPayoutsAddress(message, user, chatContext)
            ContextEntity.VIDEO -> handleAddVideo(message, user, chatContext)
            ContextEntity.GROUP -> {
                handleSetGroup(message, user, chatContext)
            }
            null -> return
        }
    }

    private fun handleSetGroup(
        message: Message,
        user: BotUser,
        chatContext: ChatContext,
    ) {
        val input = requireNotNull(message.text)

        // Clean up the input and extract username
        val username =
            input.let { text ->
                when {
                    text.contains("t.me/") ->
                        text.substringAfterLast("/").substringBefore("?").trim()
                    text.startsWith("@") -> text.substring(1).trim()
                    else -> text.trim()
                }
            }

        if (username.isEmpty()) {
            botProducer.sendTextMessage(
                message.chat.id,
                "Please send either:\n" +
                    "1. Group username (e.g., 'mygroup' or '@mygroup')\n" +
                    "2. Group link (e.g., 't.me/mygroup')",
            )
            return
        }

        // Try to get group info and connect it
        runCatching {
            val chatId = ChatId("@$username")
            val chatInfo = botApi.getChat(TelegramRequest.GetChatRequest(chatId))
            val chatType =
                chatInfo.result
                    ?.jsonObject
                    ?.get("type")
                    ?.jsonPrimitive
                    ?.content
            if (chatType !in setOf("group", "supergroup")) {
                botProducer.sendTextMessage(
                    message.chat.id,
                    "This link is not for a group. Please send a valid group link.",
                )
                return
            }

            val adminsResult =
                botApi
                    .getChatAdministrators(
                        TelegramRequest.GetChatAdministratorsRequest(
                            ChatId("@$username"),
                        ),
                    ).result
                    ?: run {
                        botProducer.sendTextMessage(
                            message.chat.id,
                            "Failed to get group administrators. Please try again.",
                        )
                        return
                    }

            val isUserAdmin =
                adminsResult.asSequence().mapNotNull { it.jsonObject }.any {
                    it["user"]
                        ?.jsonObject
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.long == user.id &&
                        it["status"]?.jsonPrimitive?.content in
                        setOf("administrator", "creator")
                }

            val botInfo = botApi.getMe()
            val botId =
                botInfo.result?.id?.longValue
                    ?: run {
                        botProducer.sendTextMessage(
                            message.chat.id,
                            "Failed to get bot information. Please try again.",
                        )
                        return
                    }

            val isBotAdmin =
                adminsResult.asSequence().mapNotNull { it.jsonObject }.any {
                    it["user"]
                        ?.jsonObject
                        ?.get("id")
                        ?.jsonPrimitive
                        ?.long == botId &&
                        it["status"]?.jsonPrimitive?.content in
                        setOf("administrator", "creator")
                }

            if (!isUserAdmin) {
                botProducer.sendTextMessage(
                    message.chat.id,
                    "You must be an administrator of the group.",
                )
                return
            }

            if (!isBotAdmin) {
                botProducer.sendTextMessage(
                    message.chat.id,
                    "The bot must be an administrator of the group.",
                )
                return
            }

            val currentChannel = requireNotNull(chatContext.channelId)
            val channel = requireNotNull(Channel.findById(currentChannel))

            // Store the group ID
            val groupId =
                chatInfo.result
                    ?.jsonObject
                    ?.get("id")
                    ?.jsonPrimitive
                    ?.long
                    ?: run {
                        botProducer.sendTextMessage(
                            message.chat.id,
                            "Failed to get group information. Please try again.",
                        )
                        return
                    }
            channel.config.connectedGroupId = groupId
            channel.persistAndFlush()

            // Clear context and show success message
            chatContext.entityName = null
            user.chatContextJson = json.encodeToString(chatContext)
            user.persistAndFlush()

            val groupTitle =
                chatInfo.result
                    ?.jsonObject
                    ?.get("title")
                    ?.jsonPrimitive
                    ?.content
                    ?: "Unknown Group"
            botProducer.sendTextMessage(
                message.chat.id,
                "Successfully connected group $groupTitle to channel ${channel.title}",
                replyKeyboardMarkup,
            )
        }.onFailure {
            log.error("Failed to process group link", it)
            botProducer.sendTextMessage(
                message.chat.id,
                "Failed to connect to the group. Please make sure:\n" +
                    "1. The group link is valid\n" +
                    "2. The bot is added to the group as an admin\n" +
                    "3. You are an admin of the group",
            )
        }
    }

    private fun handleAddSubscription(
        message: Message,
        user: BotUser,
        chatContext: ChatContext,
    ) {
        when (chatContext.modificationStep) {
            ContextModificationStep.DURATION -> {
                val duration = requireNotNull(message.text).toInt()
                val currentChannel = requireNotNull(chatContext.channelId)
                val channel = requireNotNull(Channel.findById(currentChannel))
                val subscription =
                    Subscription(durationInDays = duration, description = "", price = 0.01f)
                channel.addSubscription(subscription)
                channel.persistAndFlush()
                chatContext.entityId = subscription.id
                chatContext.modificationStep = ContextModificationStep.DESCRIPTION
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the subscription description")
            }
            ContextModificationStep.DESCRIPTION -> {
                val description = requireNotNull(message.text)
                val subscription =
                    requireNotNull(Subscription.findById(requireNotNull(chatContext.entityId)))
                subscription.description = description
                subscription.persistAndFlush()
                chatContext.modificationStep = ContextModificationStep.PRICE
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(
                    message.chat.id,
                    "Send me the subscription price in TON",
                )
            }
            ContextModificationStep.PRICE -> {
                val price = requireNotNull(message.text).toFloat()
                val subscription =
                    requireNotNull(Subscription.findById(requireNotNull(chatContext.entityId)))
                subscription.price = price
                subscription.persistAndFlush()
                chatContext.entityName = null
                chatContext.modificationStep = null
                chatContext.entityId = null
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(
                    message.chat.id,
                    "Subscription configured",
                    replyKeyboardMarkup,
                )
            }
            else -> return
        }
    }

    private fun handleSetToken(
        message: Message,
        user: BotUser,
        chatContext: ChatContext,
    ) {
        val token = requireNotNull(message.text)
        runCatching {
            val tokenBytes = token.base58.decode()
            val authToken = AuthToken.parseFrom(tokenBytes)
            require(
                authToken.payload.subject
                    .toByteArray()
                    .hex
                    .encode(true) == wallet.publicKey,
            )
            require(authToken.payload.hasBucketId())
            require(authToken.payload.canDelegate)
            require(authToken.payload.expiresAt > System.currentTimeMillis())
            chatContext.entityName = null
            user.chatContextJson = json.encodeToString(chatContext)
            user.persistAndFlush()
            val currentChannel = requireNotNull(chatContext.channelId)
            val channel = requireNotNull(Channel.findById(currentChannel))
            channel.config.botDdcAccessTokenBase58 = tokenBytes.base58.encode()
            channel.persistAndFlush()
            authToken
        }.onFailure { botProducer.sendTextMessage(message.chat.id, "Invalid token.") }
            .onSuccess {
                val expiresIn =
                    Duration
                        .ofMillis(it.payload.expiresAt - System.currentTimeMillis())
                        .toKotlinDuration()
                        .toString(DurationUnit.DAYS)
                botProducer.sendTextMessage(
                    message.chat.id,
                    "Access token is configured for channel. Bucket id is ${it.payload.bucketId}, expires in $expiresIn",
                    replyKeyboardMarkup,
                )
            }
    }

    private fun handleSetPayoutsAddress(
        message: Message,
        user: BotUser,
        chatContext: ChatContext,
    ) {
        val address = requireNotNull(message.text)
        runCatching {
            val bounceableAddress =
                tonApi
                    .detectAddress(address)
                    .result.bounceable.b64url
            chatContext.entityName = null
            user.chatContextJson = json.encodeToString(chatContext)
            user.persistAndFlush()
            val currentChannel = requireNotNull(chatContext.channelId)
            val channel = requireNotNull(Channel.findById(currentChannel))
            channel.config.payoutAddress = bounceableAddress
            channel.persistAndFlush()
        }.onFailure { botProducer.sendTextMessage(message.chat.id, "Invalid address") }
            .onSuccess {
                botProducer.sendTextMessage(
                    message.chat.id,
                    "Payouts address set to $address",
                    replyKeyboardMarkup,
                )
            }
    }

    private fun handleAddVideo(
        message: Message,
        user: BotUser,
        chatContext: ChatContext,
    ) {
        when (chatContext.modificationStep) {
            ContextModificationStep.URL -> {
                val escapedUrl =
                    UrlEscapers.urlFragmentEscaper().escape(requireNotNull(message.text))
                val urlNoQuery =
                    URI.create(requireNotNull(escapedUrl)).let {
                        "${it.scheme}://${it.host}${it.path}"
                    }

                val currentChannel = requireNotNull(chatContext.channelId)
                val channel = requireNotNull(Channel.findById(currentChannel))
                val video = Video(url = urlNoQuery)
                channel.addVideo(video)
                channel.persistAndFlush()
                chatContext.entityId = video.id
                chatContext.modificationStep = ContextModificationStep.TITLE
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the video title")
            }
            ContextModificationStep.TITLE -> {
                val title = requireNotNull(message.text)
                val video = requireNotNull(Video.findById(requireNotNull(chatContext.entityId)))
                video.title = title
                video.persistAndFlush()
                chatContext.modificationStep = ContextModificationStep.DESCRIPTION
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the video description")
            }
            ContextModificationStep.DESCRIPTION -> {
                val description = requireNotNull(message.text)
                val video = requireNotNull(Video.findById(requireNotNull(chatContext.entityId)))
                video.description = description
                video.persistAndFlush()
                chatContext.modificationStep = ContextModificationStep.THUMBNAIL
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the video thumbnail URL")
            }
            ContextModificationStep.THUMBNAIL -> {
                val thumbnailUrl = requireNotNull(message.text)
                val video = requireNotNull(Video.findById(requireNotNull(chatContext.entityId)))
                video.thumbnailUrl = thumbnailUrl
                video.persistAndFlush()
                chatContext.entityName = null
                chatContext.modificationStep = null
                chatContext.entityId = null
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(
                    message.chat.id,
                    "Video added to the channel",
                    replyKeyboardMarkup,
                )
            }
            else -> return
        }
    }
}
