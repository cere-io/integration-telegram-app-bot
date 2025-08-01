package network.cere.telegram.bot

import com.google.protobuf.ByteString
import com.iwebpp.crypto.TweetNaclFast
import io.cere.etl.*
import io.cere.file.*
import io.cere.file.FileApiGrpc
import io.grpc.Channel
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcClient
import jakarta.enterprise.context.ApplicationScoped
import network.cere.ddc.crypto.v1.key.sign.signingKeyPairFromMnemonic
import org.bitcoinj.core.Base58
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.ipfs.multibase.Multibase

@ApplicationScoped
class TelegramFileService(
    @GrpcClient("ddc") private val channel: Channel,
    @ConfigProperty(name = "ddc.bucket") private val bucket: Long,
    @ConfigProperty(name = "ddc.mnemonic") private val mnemonic: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_CHUNK_SIZE = 64 * 1024 // 64KB chunks
        private const val MAX_PIECE_SIZE = 64 * 1024 * 1024 // 64MB max piece size
    }

    /**
     * Store file using File API (putRawPiece) - more efficient for files
     */
    fun storeFile(fileData: ByteArray): String {
        log.info("Storing file of size {} bytes in DDC via File API", fileData.size)

        if (fileData.size > MAX_PIECE_SIZE) {
            throw IllegalArgumentException("File size ${fileData.size} exceeds maximum ${MAX_PIECE_SIZE}")
        }

        val keyPair = signingKeyPairFromMnemonic(mnemonic)
        val signer = keyPair.publicKey.encoded

        // Create auth metadata
        val metadata = createAuthMetadata(signer, keyPair.privateKey.encoded, fileData.size.toLong())
        val fileApi = FileApiGrpc.newStub(channel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))

        val latch = CountDownLatch(1)
        var result: String? = null
        var error: Throwable? = null

        val responseObserver = object : StreamObserver<PutRawPieceResponse> {
            override fun onNext(response: PutRawPieceResponse) {
                try {
                    val cidBytes = response.cid.toByteArray()
                    log.info("Raw CID bytes: {}", cidBytes.contentToString())
                    log.info("Raw CID bytes as hex: {}", cidBytes.joinToString("") { "%02x".format(it) })
                    log.info("Raw CID bytes length: {}", cidBytes.size)

                    // Encode CID using multibase
                    result = if (cidBytes.isNotEmpty()) {
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
                                "f" + cidBytes.joinToString("") { "%02x".format(it) }
                            }
                        }
                    } else {
                        throw IllegalStateException("Empty CID bytes received")
                    }

                    log.info("File stored in DDC via File API. Final CID: {}", result)
                } catch (e: Exception) {
                    error = e
                }
            }

            override fun onError(t: Throwable) {
                log.error("Error storing file via File API", t)
                error = t
                latch.countDown()
            }

            override fun onCompleted() {
                latch.countDown()
            }
        }

        val requestObserver = fileApi.putRawPiece(responseObserver)

        try {
            // Send metadata first
            val metadata = PutRawPieceRequest.Metadata.newBuilder()
                .setBucketId(bucket)
                .setIsMultipart(false)
                .setOffset(0)
                .setSize(fileData.size.toLong())
                .build()

            val metadataRequest = PutRawPieceRequest.newBuilder()
                .setMetadata(metadata)
                .build()

            requestObserver.onNext(metadataRequest)

            // Send file data in chunks
            var offset = 0
            while (offset < fileData.size) {
                val chunkSize = minOf(MAX_CHUNK_SIZE, fileData.size - offset)
                val chunk = fileData.copyOfRange(offset, offset + chunkSize)

                val content = PutRawPieceRequest.Content.newBuilder()
                    .setData(ByteString.copyFrom(chunk))
                    .build()

                val contentRequest = PutRawPieceRequest.newBuilder()
                    .setContent(content)
                    .build()

                requestObserver.onNext(contentRequest)
                offset += chunkSize
            }

            requestObserver.onCompleted()

            // Wait for response
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw RuntimeException("Timeout waiting for putRawPiece response")
            }

            error?.let { throw it }
            return result ?: throw RuntimeException("No CID received from putRawPiece")

        } catch (e: Exception) {
            requestObserver.onError(e)
            throw e
        }
    }

    /**
     * Get file from DDC using File API
     */
    fun getFile(cid: String): ByteArray {
        log.info("Getting file from DDC via File API. CID: {}", cid)

        val keyPair = signingKeyPairFromMnemonic(mnemonic)
        val signer = keyPair.publicKey.encoded

        // Create auth metadata for GET operation
        val metadata = createAuthMetadata(signer, keyPair.privateKey.encoded, 0L, isGetOperation = true)
        val fileApi = FileApiGrpc.newStub(channel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))

        val latch = CountDownLatch(1)
        var result: ByteArray? = null
        var error: Throwable? = null

        val responseObserver = object : StreamObserver<GetFileResponse> {
            private val dataChunks = mutableListOf<ByteArray>()

            override fun onNext(response: GetFileResponse) {
                when (response.bodyCase) {
                    GetFileResponse.BodyCase.PROOF -> {
                        log.debug("Received proof from server")
                        // Handle proof if needed
                    }
                    GetFileResponse.BodyCase.DATA -> {
                        val chunk = response.data.toByteArray()
                        dataChunks.add(chunk)
                        log.debug("Received data chunk of size: {}", chunk.size)
                    }
                    else -> {
                        log.warn("Received unknown response type")
                    }
                }
            }

            override fun onError(t: Throwable) {
                log.error("Error getting file via File API", t)
                error = t
                latch.countDown()
            }

            override fun onCompleted() {
                // Concatenate all chunks
                val totalSize = dataChunks.sumOf { it.size }
                result = ByteArray(totalSize)
                var offset = 0
                for (chunk in dataChunks) {
                    System.arraycopy(chunk, 0, result, offset, chunk.size)
                    offset += chunk.size
                }
                log.info("File retrieved from DDC via File API. Total size: {} bytes", totalSize)
                latch.countDown()
            }
        }

        val requestObserver = fileApi.getFile(responseObserver)

        try {
            // Convert CID to bytes
            val cidBytes = try {
                Multibase.decode(cid)
            } catch (e: Exception) {
                log.warn("Failed to decode CID as multibase, using UTF-8: {}", e.message)
                cid.toByteArray(Charsets.UTF_8)
            }

            val getRequest = GetFileRequest.Request.newBuilder()
                .setCid(ByteString.copyFrom(cidBytes))
                .setBucketId(bucket)
                .setAuthenticate(false)
                .build()

            val request = GetFileRequest.newBuilder()
                .setRequest(getRequest)
                .build()

            requestObserver.onNext(request)
            requestObserver.onCompleted()

            // Wait for response
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw RuntimeException("Timeout waiting for getFile response")
            }

            error?.let { throw it }
            return result ?: throw RuntimeException("No data received from getFile")

        } catch (e: Exception) {
            requestObserver.onError(e)
            throw e
        }
    }

    private fun createAuthMetadata(
        signer: ByteArray, 
        privateKey: ByteArray, 
        size: Long, 
        isGetOperation: Boolean = false
    ): io.grpc.Metadata {
        val payload = Payload.newBuilder()
            .setBucketId(bucket)
            .setExpiresAt(System.currentTimeMillis() + 1000 * 60) // 1 minute
            .setCanDelegate(false)
            .addOperations(if (isGetOperation) Operation.GET else Operation.PUT)
            .build()

        val unsignedAuthToken = AuthToken.newBuilder()
            .setPayload(payload)
            .build()

        val signatureValue = TweetNaclFast.Signature(null, privateKey)
            .detached(unsignedAuthToken.toByteArray())

        val signature = Signature.newBuilder()
            .setValue(ByteString.copyFrom(signatureValue))
            .setSigner(ByteString.copyFrom(signer))
            .setAlgorithm(Signature.Algorithm.ED_25519)
            .build()

        val signedAuthToken = unsignedAuthToken.toBuilder().setSignature(signature).build()

        val activityRequest = ActivityRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setRequestType(if (isGetOperation) ActivityRequest.RequestType.REQUEST_TYPE_GET else ActivityRequest.RequestType.REQUEST_TYPE_PUT)
            .setContentType(ActivityRequest.ContentType.CONTENT_TYPE_PIECE)
            .setBucketId(bucket)
            .apply { if (!isGetOperation) setSize(size) }
            .setTimestamp(Instant.now().toEpochMilli())
            .build()

        val activityRequestSignatureValue = TweetNaclFast.Signature(null, privateKey)
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
        metadata.put(
            io.grpc.Metadata.Key.of("token", io.grpc.Metadata.ASCII_STRING_MARSHALLER),
            Base58.encode(signedAuthToken.toByteArray())
        )
        metadata.put(
            io.grpc.Metadata.Key.of("request", io.grpc.Metadata.ASCII_STRING_MARSHALLER),
            Base64.getEncoder().encodeToString(signedActivityRequest.toByteArray())
        )

        return metadata
    }
}