package network.cere.telegram.bot.streaming.webhook.command.impl.text

import com.github.omarmiatello.telegram.Message
import com.github.omarmiatello.telegram.Update
import dev.sublab.base58.base58
import dev.sublab.hex.hex
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.cere.ddc.AuthToken
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
) {
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
            null -> return
        }
    }

    private fun handleAddSubscription(message: Message, user: BotUser, chatContext: ChatContext) {
        when (chatContext.modificationStep) {
            ContextModificationStep.DURATION -> {
                val duration = requireNotNull(message.text).toInt()
                val currentChannel = requireNotNull(chatContext.channelId)
                val channel = requireNotNull(Channel.findById(currentChannel))
                val subscription = Subscription(durationInDays = duration, description = "", price = 0.01f)
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
                val subscription = requireNotNull(Subscription.findById(requireNotNull(chatContext.entityId)))
                subscription.description = description
                subscription.persistAndFlush()
                chatContext.modificationStep = ContextModificationStep.PRICE
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the subscription price in TON")
            }
            ContextModificationStep.PRICE -> {
                val price = requireNotNull(message.text).toFloat()
                val subscription = requireNotNull(Subscription.findById(requireNotNull(chatContext.entityId)))
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
                    replyKeyboardMarkup
                )
            }
            else -> return
        }
    }

    private fun handleSetToken(message: Message, user: BotUser, chatContext: ChatContext) {
        val token = requireNotNull(message.text)
        runCatching {
            val tokenBytes = token.base58.decode()
            val authToken = AuthToken.parseFrom(tokenBytes)
            require(authToken.payload.subject.toByteArray().hex.encode(true) == wallet.publicKey)
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
        }.onFailure {
            botProducer.sendTextMessage(message.chat.id, "Invalid token.")
        }.onSuccess {
            val expiresIn = Duration.ofMillis(it.payload.expiresAt - System.currentTimeMillis())
                .toKotlinDuration()
                .toString(DurationUnit.DAYS)
            botProducer.sendTextMessage(
                message.chat.id,
                "Access token is configured for channel. Bucket id is ${it.payload.bucketId}, expires in $expiresIn",
                replyKeyboardMarkup
            )
        }
    }

    private fun handleSetPayoutsAddress(message: Message, user: BotUser, chatContext: ChatContext) {
        val address = requireNotNull(message.text)
        runCatching {
            val bounceableAddress = tonApi.detectAddress(address).result.bounceable.b64url
            chatContext.entityName = null
            user.chatContextJson = json.encodeToString(chatContext)
            user.persistAndFlush()
            val currentChannel = requireNotNull(chatContext.channelId)
            val channel = requireNotNull(Channel.findById(currentChannel))
            channel.config.payoutAddress = bounceableAddress
            channel.persistAndFlush()
        }.onFailure {
            botProducer.sendTextMessage(message.chat.id, "Invalid address")
        }.onSuccess {
            botProducer.sendTextMessage(
                message.chat.id,
                "Payouts address set to $address",
                replyKeyboardMarkup
            )
        }
    }

    private fun handleAddVideo(message: Message, user: BotUser, chatContext: ChatContext) {
        when (chatContext.modificationStep) {
            ContextModificationStep.URL -> {
                val url = URI.create(requireNotNull(message.text))
                    .let { "${it.scheme}://${it.host}${it.path}" }
                val currentChannel = requireNotNull(chatContext.channelId)
                val channel = requireNotNull(Channel.findById(currentChannel))
                val video = Video(url = url)
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
                    replyKeyboardMarkup
                )
            }

            else -> return
        }
    }
}