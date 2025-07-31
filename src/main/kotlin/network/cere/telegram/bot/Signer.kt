package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

@ApplicationScoped
class Signer(config: Config) {
    private val privateKeyParams = Ed25519PrivateKeyParameters(
        config.privateKey()
            .substringAfter('x')
            .decodeHex(),
        0 // offset
    )

    val publicKey = privateKeyParams.generatePublicKey()
        .encoded
        .hex()

    fun sign(data: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, privateKeyParams)
        signer.update(data, 0, data.size)
        return signer.generateSignature()
    }
}
