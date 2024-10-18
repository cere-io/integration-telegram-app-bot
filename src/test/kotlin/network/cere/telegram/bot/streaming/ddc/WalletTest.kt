package network.cere.telegram.bot.streaming.ddc

import dev.sublab.encrypting.keys.KeyPair
import dev.sublab.encrypting.mnemonic.DefaultMnemonic
import dev.sublab.hex.hex
import dev.sublab.sr25519.sr25519
import network.cere.ddc.AuthToken
import network.cere.ddc.Operation
import network.cere.ddc.Payload
import network.cere.ddc.Signature
import java.time.Duration

class WalletTest

fun main() {
    val botpk = "0x08fc09ba94bc4778cbe9fdb539abb549143b42b74fb124167a0d9ad2c03b1404"
    val keyPair = KeyPair.Factory.sr25519()
        .generate(DefaultMnemonic.fromPhrase("mutual cattle skirt first polar isolate celery public frown nuclear stage maximum"))
    val payload = Payload.newBuilder()
        .setBucketId(254117)
        .setSubject(botpk.hex.decode().toByteString())
        .setExpiresAt(System.currentTimeMillis() + Duration.ofDays(365).toMillis())
        .setCanDelegate(true)
        .addOperations(Operation.GET)
        .build()
    val unsignedAuthToken = AuthToken.newBuilder()
        .setPayload(payload)
        .build()
    val signatureValue = keyPair.sign(unsignedAuthToken.toByteArray())
    val signature = Signature.newBuilder()
        .setValue(signatureValue.toByteString())
        .setSigner(keyPair.publicKey.toByteString())
        .setAlgorithm(Signature.Algorithm.SR_25519)
        .build()
    unsignedAuthToken.toBuilder()
        .setSignature(signature)
        .build()
        .toByteArray()
        .hex
        .encode(true)
        .also { println(it) }
}