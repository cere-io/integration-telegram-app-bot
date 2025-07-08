package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.math.ec.rfc8032.Ed25519.Algorithm

@ApplicationScoped
class Signer(config: Config) {
    private val signer = config.privateKey()
        .substringAfter('x')
        .decodeHex()
        .let(::Ed25519PrivateKeyParameters)
    val publicKey = signer.generatePublicKey()
        .encoded
        .hex()

    fun sign(data: ByteArray): ByteArray {
        val signatureBuffer = ByteArray(Ed25519PrivateKeyParameters.SIGNATURE_SIZE)
        signer.sign(Algorithm.Ed25519, null, data, 0, data.size, signatureBuffer, 0)
        return signatureBuffer
    }
}
