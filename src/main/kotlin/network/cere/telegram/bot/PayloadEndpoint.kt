package network.cere.telegram.bot

import io.smallrye.common.annotation.RunOnVirtualThread
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import kotlinx.serialization.json.JsonElement
import org.slf4j.LoggerFactory

@Path("/api/payload")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PayloadEndpoint(
    private val payloadAnalyzer: PayloadAnalyzer,
    private val messageFormatter: MessageFormatter,
    private val chatNotificationService: ChatNotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @POST
    @Path("/send")
    @RunOnVirtualThread
    fun sendPayload(request: PayloadRequest): Response {
        log.info("Received payload request - source: {}, priority: {}, targetChats: {}", 
            request.source, request.priority, request.targetChats)
        
        try {
            // Analyze the payload
            val analysis = payloadAnalyzer.analyze(request.payload)
            log.debug("Payload analyzed as type: {}", analysis.type)
            
            // Format the message
            val formattedMessage = messageFormatter.formatMessage(
                analysis = analysis,
                source = request.source,
                priority = request.priority
            )
            log.debug("Message formatted, length: {} characters", formattedMessage.length)
            
            // Send to chats
            val notificationResult = chatNotificationService.sendToChats(
                message = formattedMessage,
                targetChats = request.targetChats
            )
            
            // Build response
            val response = PayloadResponse(
                success = notificationResult.successCount > 0,
                message = if (notificationResult.successCount > 0) {
                    "Payload processed and sent to ${notificationResult.successCount}/${notificationResult.successCount + notificationResult.errors.size} chats"
                } else {
                    "Failed to send payload to any chats"
                },
                processedType = analysis.type.name,
                sentToChatCount = notificationResult.successCount,
                errors = notificationResult.errors
            )
            
            log.info("Payload processing completed - success: {}, sent to {} chats", 
                response.success, response.sentToChatCount)
            
            return Response.ok(response).build()
            
        } catch (e: Exception) {
            log.error("Error processing payload request", e)
            val errorResponse = PayloadResponse(
                success = false,
                message = "Internal error: ${e.message}",
                processedType = "ERROR",
                sentToChatCount = 0,
                errors = listOf(e.message ?: "Unknown error")
            )
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build()
        }
    }

    @POST
    @Path("/send-json")
    @RunOnVirtualThread
    fun sendJson(
        payload: JsonElement,
        @QueryParam("source") source: String? = null,
        @QueryParam("priority") priority: String? = null,
        @QueryParam("target_chats") targetChats: String? = null
    ): Response {
        log.info("Received JSON payload request - source: {}, priority: {}", source, priority)
        
        val parsedPriority = try {
            priority?.let { PayloadPriority.valueOf(it.uppercase()) } ?: PayloadPriority.NORMAL
        } catch (e: Exception) {
            log.warn("Invalid priority '{}', using NORMAL", priority)
            PayloadPriority.NORMAL
        }
        
        val parsedTargetChats = targetChats?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        
        val request = PayloadRequest(
            payload = payload,
            source = source,
            targetChats = parsedTargetChats,
            priority = parsedPriority
        )
        
        return sendPayload(request)
    }

    @GET
    @Path("/health")
    fun health(): Response {
        val subscribedChats = chatNotificationService.getConfiguredChatIds()
        
        val healthInfo = mapOf(
            "status" to "UP",
            "service" to "Payload Processing API",
            "subscribed_groups" to subscribedChats.size,
            "subscribed_group_ids" to subscribedChats,
            "supported_types" to PayloadType.values().size,
            "note" to "Groups are discovered dynamically when bot receives messages"
        )
        
        log.debug("PayloadEndpoint.kt: Health check - subscribed groups: {}", subscribedChats.size)
        return Response.ok(healthInfo).build()
    }

    @GET
    @Path("/types")
    fun getSupportedTypes(): Response {
        val types = PayloadType.values().associate { type ->
            type.name to mapOf(
                "description" to type.description,
                "icon" to type.icon
            )
        }
        
        log.debug("Returning {} supported payload types", types.size)
        return Response.ok(types).build()
    }
} 