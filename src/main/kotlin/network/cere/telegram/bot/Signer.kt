package network.cere.telegram.bot

import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.KeyGenerationParameters
import java.security.SecureRandom

@ApplicationScoped
class Signer(config: Config) {
    private val privateKeyBytes = config.privateKey()
        .substringAfter('x')
        .decodeHex()
    
    private val privateKeyParams: Ed25519PrivateKeyParameters = try {
        // Try the new constructor first
        Ed25519PrivateKeyParameters(privateKeyBytes)
    } catch (e: NoSuchMethodError) {
        // Fallback to old constructor if available
        try {
            Ed25519PrivateKeyParameters(privateKeyBytes, 0)
        } catch (e2: Exception) {
            // Last resort: use key factory
            PrivateKeyFactory.createKey(privateKeyBytes) as Ed25519PrivateKeyParameters
        }
    }

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
