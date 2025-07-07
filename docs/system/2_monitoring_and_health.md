# System: Monitoring and Health

This document outlines the monitoring and health check strategy for the bot, which is crucial for maintaining reliability in a production environment.

## Health Check Architecture Diagram

```mermaid
graph TD
    subgraph "External Systems"
        MONITOR[Monitoring Service<br>(e.g., Prometheus)]
        ORCHESTRATOR[Container Orchestrator<br>(e.g., Kubernetes, ECS)]
    end

    subgraph "Application"
        HEALTH_CTRL[HealthCheckController]
        HEALTH_SVC[HealthCheckService]
    end
    
    subgraph "Internal Services"
        UNIFIED_SDK[UnifiedSdkService]
        TG_SERVICE[TelegramBotService]
        CONFIG[ConfigService]
    end

    MONITOR -- "GET /health (Comprehensive)" --> HEALTH_CTRL
    MONITOR -- "GET /metrics (Prometheus)" --> HEALTH_CTRL
    
    ORCHESTRATOR -- "GET /health/live (Liveness)" --> HEALTH_CTRL
    ORCHESTRATOR -- "GET /health/ready (Readiness)" --> HEALTH_CTRL

    HEALTH_CTRL --> HEALTH_SVC

    HEALTH_SVC --> UNIFIED_SDK
    HEALTH_SVC --> TG_SERVICE
    HEALTH_SVC --> CONFIG
```

## Health Check Strategy

The bot implements a comprehensive health check strategy with multiple endpoints, each serving a distinct purpose.

### 1. Liveness Probe (`/health/live`)

*   **Purpose**: To indicate if the application process is running. If this endpoint fails, the container orchestrator should restart the container.
*   **Logic**: This is a simple check that returns `HTTP 200 OK` if the NestJS application is up and responding to requests. It does not check dependencies.

### 2. Readiness Probe (`/health/ready`)

*   **Purpose**: To indicate if the application is ready to start accepting traffic. If this probe fails, the orchestrator will not route traffic to this instance.
*   **Logic**: This check is more thorough. It verifies that:
    *   The `UnifiedSdkService` has been successfully initialized.
    *   Required configurations (like `TELEGRAM_BOT_TOKEN`) are present.
    *   The application is in a state where it can successfully process incoming webhooks.

### 3. Comprehensive Health Check (`/health`)

*   **Purpose**: For external monitoring systems (like Prometheus or a status page) to get a detailed view of the bot's health and the status of its dependencies.
*   **Logic**: This endpoint, managed by the `HealthCheckService`, performs a full check on all critical components:
    *   **Unified SDK**: Checks if the SDK is initialized and can communicate with Cere's backend.
    *   **Telegram Bot Service**: Verifies that the Telegram bot token is configured.
    *   **Configuration**: Ensures all required environment variables are loaded.
*   **Response**: It returns a detailed JSON object with the status (`healthy`, `degraded`, `unhealthy`) of each component.

## Key Metrics to Monitor

For operational excellence, the following key metrics should be monitored:

*   **Webhook Request Rate**: The number of incoming requests from Telegram per minute. Spikes can indicate high group activity.
*   **Webhook Error Rate (4xx and 5xx)**: The percentage of webhook requests that result in an error. An increase in `5xx` errors indicates a problem with the bot itself.
*   **Event Dispatch Latency**: The time it takes for the `UnifiedSdkService` to send an event to the Cere Activity SDK. An increase in this metric could indicate network issues or problems with the downstream service.
*   **Event Dispatch Success/Failure Rate**: The number of successful vs. failed attempts to send events to Cere. A high failure rate is a critical alert.
*   **CPU and Memory Utilization**: Standard container metrics to ensure the bot is not resource-constrained and to inform scaling decisions.
