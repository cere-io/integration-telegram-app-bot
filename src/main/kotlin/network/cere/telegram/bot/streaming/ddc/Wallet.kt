package network.cere.telegram.bot.streaming.ddc

import dev.sublab.base58.StringBase58
import dev.sublab.base58.base58
import dev.sublab.ed25519.ed25519
import dev.sublab.encrypting.keys.KeyPair
import dev.sublab.encrypting.mnemonic.DefaultMnemonic
import dev.sublab.hex.hex
import dev.sublab.sr25519.sr25519
import jakarta.enterprise.context.ApplicationScoped
import network.cere.ddc.AuthToken
import network.cere.ddc.Operation
import network.cere.ddc.Payload
import network.cere.ddc.Signature

@ApplicationScoped
class Wallet(private val ddcConfig: DdcConfig) {
    private companion object {
        //TODO make configurable
        private const val TOKEN_DURATION = 60L * 60 * 1000 // 1 hour
    }

    private val keyPair = when (ddcConfig.wallet().algorithm()) {
        Signature.Algorithm.ED_25519 -> KeyPair.Factory.ed25519.generate(DefaultMnemonic.fromPhrase(ddcConfig.wallet().mnemonic()))
        Signature.Algorithm.SR_25519 -> KeyPair.Factory.sr25519().generate(DefaultMnemonic.fromPhrase(ddcConfig.wallet().mnemonic()))
        Signature.Algorithm.UNRECOGNIZED -> throw IllegalArgumentException("Unrecognized signature algorithm")
    }

    val publicKey = keyPair.publicKey.hex.encode(true)

    fun grantAccess(botDdcAccessTokenBase58: String): String {
        val botToken = StringBase58(botDdcAccessTokenBase58)
            .toByteString()
            .toByteArray()
            .let(AuthToken::parseFrom)
        val payload = Payload.newBuilder()
            .setPrev(botToken)
            .setExpiresAt(System.currentTimeMillis() + TOKEN_DURATION)
            .setCanDelegate(false)
            .addOperations(Operation.GET)
            .build()
        val unsignedAuthToken = AuthToken.newBuilder()
            .setPayload(payload)
            .build()
        val signatureValue = keyPair.sign(unsignedAuthToken.toByteArray())
        val signature = Signature.newBuilder()
            .setValue(signatureValue.toByteString())
            .setSigner(keyPair.publicKey.toByteString())
            .setAlgorithm(ddcConfig.wallet().algorithm())
            .build()
        return unsignedAuthToken.toBuilder()
            .setSignature(signature)
            .build()
            .toByteArray()
            .base58
            .encode()
    }
}