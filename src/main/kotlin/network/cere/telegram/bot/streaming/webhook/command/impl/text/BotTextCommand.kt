package network.cere.telegram.bot.streaming.webhook.command.impl.text

import com.github.omarmiatello.telegram.InlineKeyboardButton
import com.github.omarmiatello.telegram.InlineKeyboardMarkup
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
import network.cere.telegram.bot.streaming.ton.TonApi
import network.cere.telegram.bot.streaming.user.BotUser
import network.cere.telegram.bot.streaming.user.ChatContext
import network.cere.telegram.bot.streaming.video.Video
import network.cere.telegram.bot.streaming.webhook.BotProducer
import org.eclipse.microprofile.rest.client.inject.RestClient
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
    //TODO check if admin
    fun tryHandle(update: Update) {
        if (update.message?.text == null) return

        val message = requireNotNull(update.message)
        val from = requireNotNull(message.from)
        val user = requireNotNull(BotUser.findById(from.id.longValue))
        val chatContext = json.decodeFromString<ChatContext>(user.chatContextJson)

        when (chatContext.entityName) {
            "token" -> handleSetToken(message, user, chatContext)
            "payoutsAddress" -> handleSetPayoutsAddress(message, user, chatContext)
            "video" -> handleAddVideo(message, user, chatContext)
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
            // TODO replace hardcoded commands
            botProducer.sendTextMessage(
                message.chat.id,
                "Access token is configured for channel. Bucket id is ${it.payload.bucketId}, expires in $expiresIn",
                InlineKeyboardMarkup(
                    inline_keyboard = listOf(
                        listOf(
                            InlineKeyboardButton(
                                text = "Set Bot Access Token",
                                callback_data = "/setToken"
                            ),
                            InlineKeyboardButton(
                                text = "Set payouts address",
                                callback_data = "/setPayoutsAddress"
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                text = "Add video",
                                callback_data = "/addVideo"
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                text = "Check configuration",
                                callback_data = "/check"
                            ),
                        )
                    )
                )
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
            // TODO replace hardcoded commands
            botProducer.sendTextMessage(
                message.chat.id,
                "Payouts address set to $address",
                InlineKeyboardMarkup(
                    inline_keyboard = listOf(
                        listOf(
                            InlineKeyboardButton(
                                text = "Set Bot Access Token",
                                callback_data = "/setToken"
                            ),
                            InlineKeyboardButton(
                                text = "Set payouts address",
                                callback_data = "/setPayoutsAddress"
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                text = "Add video",
                                callback_data = "/addVideo"
                            ),
                        ),
                        listOf(
                            InlineKeyboardButton(
                                text = "Check configuration",
                                callback_data = "/check"
                            ),
                        )
                    )
                )
            )
        }
    }

    private fun handleAddVideo(message: Message, user: BotUser, chatContext: ChatContext) {
        when (chatContext.modificationStep) {
            "url" -> {
                val url = requireNotNull(message.text)
                //TODO validate URL
                val currentChannel = requireNotNull(chatContext.channelId)
                val channel = requireNotNull(Channel.findById(currentChannel))
                val video = Video(url = url)
                channel.addVideo(video)
                channel.persistAndFlush()
                chatContext.entityId = video.id
                chatContext.modificationStep = "title"
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the video title")
            }

            "title" -> {
                val title = requireNotNull(message.text)
                val video = requireNotNull(Video.findById(requireNotNull(chatContext.entityId)))
                video.title = title
                video.persistAndFlush()
                chatContext.modificationStep = "description"
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the video description")
            }

            "description" -> {
                val description = requireNotNull(message.text)
                val video = requireNotNull(Video.findById(requireNotNull(chatContext.entityId)))
                video.description = description
                video.persistAndFlush()
                chatContext.modificationStep = "thumbnail"
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                botProducer.sendTextMessage(message.chat.id, "Send me the video thumbnail URL")
            }

            "thumbnail" -> {
                val thumbnailUrl = requireNotNull(message.text)
                //TODO validate URL
                val video = requireNotNull(Video.findById(requireNotNull(chatContext.entityId)))
                video.thumbnailUrl = thumbnailUrl
                video.persistAndFlush()
                chatContext.entityName = null
                chatContext.modificationStep = null
                chatContext.entityId = null
                user.chatContextJson = json.encodeToString(chatContext)
                user.persistAndFlush()
                // TODO replace hardcoded commands
                botProducer.sendTextMessage(
                    message.chat.id,
                    "Video added to the channel",
                    InlineKeyboardMarkup(
                        inline_keyboard = listOf(
                            listOf(
                                InlineKeyboardButton(
                                    text = "Set Bot Access Token",
                                    callback_data = "/setToken"
                                ),
                                InlineKeyboardButton(
                                    text = "Set payouts address",
                                    callback_data = "/setPayoutsAddress"
                                ),
                            ),
                            listOf(
                                InlineKeyboardButton(
                                    text = "Add video",
                                    callback_data = "/addVideo"
                                ),
                            ),
                            listOf(
                                InlineKeyboardButton(
                                    text = "Check configuration",
                                    callback_data = "/check"
                                ),
                            )
                        )
                    )
                )
            }
        }
    }
}