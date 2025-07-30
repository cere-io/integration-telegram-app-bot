package network.cere.telegram.bot

import com.google.protobuf.ByteString
import com.iwebpp.crypto.TweetNaclFast
import io.cere.etl.*
import io.cere.etl.DagApiGrpc.DagApiBlockingStub
import io.grpc.stub.MetadataUtils
import io.quarkus.grpc.GrpcClient
import jakarta.enterprise.context.ApplicationScoped
import network.cere.ddc.crypto.v1.key.sign.signingKeyPairFromMnemonic
import org.bitcoinj.core.Base58
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import io.ipfs.multibase.Multibase

@ApplicationScoped
class TelegramDdcService(
    @GrpcClient("ddc") private val ddc: DagApiBlockingStub,
    @ConfigProperty(name = "ddc.bucket") private val bucket: Long,
    @ConfigProperty(name = "ddc.mnemonic") private val mnemonic: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Store binary file data in DDC (for images, documents, etc.)
     */
    fun storeFile(fileData: ByteArray, lastCid: String? = null): String {
        log.info("Storing file of size {} bytes in DDC via gRPC", fileData.size)
        
        val dagNode = Node.newBuilder()
            .setData(ByteString.copyFrom(fileData))
        if (lastCid != null) {
            runCatching {
                dagNode.addLinks(
                    Link.newBuilder()
                        .setCid(ByteString.copyFrom(Base64.getDecoder().decode(lastCid)))
                )
            }
        }
        val finalDagNode = dagNode.build()
            
        val request = PutRequest.newBuilder()
            .setBucketId(bucket)
            .setNode(finalDagNode)
            .build()

        val keyPair = signingKeyPairFromMnemonic(mnemonic)
        val signer = keyPair.publicKey.encoded

        val payload = Payload.newBuilder()
            .setBucketId(bucket)
            .setExpiresAt(System.currentTimeMillis() + 1000 * 60)
            .setCanDelegate(false)
            .addOperations(Operation.PUT)
            .build()

        val unsignedAuthToken = AuthToken.newBuilder()
            .setPayload(payload)
            .build()

        val signatureValue = TweetNaclFast.Signature(null, keyPair.privateKey.encoded)
            .detached(unsignedAuthToken.toByteArray())

        val signature = Signature.newBuilder()
            .setValue(ByteString.copyFrom(signatureValue))
            .setSigner(ByteString.copyFrom(signer))
            .setAlgorithm(Signature.Algorithm.ED_25519)
            .build()

        val signedAuthToken = unsignedAuthToken.toBuilder().setSignature(signature).build()

        val activityRequest = ActivityRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setRequestType(ActivityRequest.RequestType.REQUEST_TYPE_PUT)
            .setContentType(ActivityRequest.ContentType.CONTENT_TYPE_PIECE)
            .setBucketId(bucket)
            .setSize(finalDagNode.data.size().toLong())
            .setTimestamp(Instant.now().toEpochMilli())
            .build()
        val activityRequestSignatureValue = TweetNaclFast.Signature(null, keyPair.privateKey.encoded)
            .detached(activityRequest.toByteArray())
        val activityRequestSignature = Signature.newBuilder()
            .setValue(ByteString.copyFrom(activityRequestSignatureValue))
            .setSigner(ByteString.copyFrom(signer))
            .setAlgorithm(Signature.Algorithm.ED_25519)
            .build()
        val signedActivityRequest = activityRequest.toBuilder()
            .setSignature(activityRequestSignature)
            .build()

        val metadata = io.grpc.Metadata()

        metadata.put(io.grpc.Metadata.Key.of("token", io.grpc.Metadata.ASCII_STRING_MARSHALLER), Base58.encode(signedAuthToken.toByteArray()))
        metadata.put(
            io.grpc.Metadata.Key.of("request", io.grpc.Metadata.ASCII_STRING_MARSHALLER),
            Base64.getEncoder().encodeToString(signedActivityRequest.toByteArray())
        )

        val ddcWithAuth = ddc.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))

        return runCatching {
            val response = ddcWithAuth.put(request)
            val cidBytes = response.cid.toByteArray()
            log.info("Raw CID bytes: {}", cidBytes.contentToString())
            log.info("Raw CID bytes as hex: {}", cidBytes.joinToString("") { "%02x".format(it) })
            log.info("Raw CID bytes length: {}", cidBytes.size)
            
            // CID is structured data that needs proper multibase encoding
            // The first byte should indicate the multibase encoding
            val cid = if (cidBytes.isNotEmpty()) {
                // Try to encode using base32 (most common for CID)
                try {
                    val base32cid = Multibase.encode(Multibase.Base.Base32, cidBytes)
                    log.info("Encoded CID with base32: {}", base32cid)
                    base32cid
                } catch (e: Exception) {
                    log.warn("Failed to encode with base32, trying base58btc: {}", e.message)
                    try {
                        val base58cid = Multibase.encode(Multibase.Base.Base58BTC, cidBytes)
                        log.info("Encoded CID with base58btc: {}", base58cid)
                        base58cid
                    } catch (e2: Exception) {
                        log.error("Failed to encode CID with any base: {}", e2.message)
                        // Fallback to hex encoding for debugging
                        "f" + cidBytes.joinToString("") { "%02x".format(it) }
                    }
                }
            } else {
                throw IllegalStateException("Empty CID bytes received")
            }
            
            log.info("File stored in DDC via gRPC. Final CID: {}", cid)
            cid
        }.onFailure {
            log.error("Unable to store file in DDC via gRPC", it)
        }.getOrThrow()
    }

    /**
     * Store text data (like messages) in DDC
     */
    fun storeText(text: String): String {
        return storeFile(text.toByteArray())
    }
}