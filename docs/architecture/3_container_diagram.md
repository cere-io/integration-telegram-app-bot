# Container Diagram

This document provides a C4-style container diagram that illustrates the deployment architecture of the **Integration Telegram App Bot**. It shows the running container, its internal processes, and its connections to other systems in a production-like environment.

## Deployment Diagram

```mermaid
graph TD
    subgraph "Internet"
        direction LR
        TELEGRAM_API[Telegram API]
        MONITORING[Monitoring Service<br>(e.g., Prometheus)]
    end

    subgraph "AWS VPC"
        direction TB
        LB[Load Balancer]

        subgraph "ECS / EKS Cluster"
            direction LR
            
            CONTAINER[
                <b>Container: integration-telegram-app-bot-ts</b>
                ---
                <i>Node.js 20 Alpine</i>
                ---
                <b>Process: NestJS Application</b><br>
                - Listens on port 8080<br>
                - Exposes /telegram/webhook<br>
                - Exposes /health endpoints
            ]
        end
    end

    subgraph "Cere Network"
        CERE_SDK[Cere Activity SDK API]
    end

    TELEGRAM_API -- "Webhook POST" --> LB
    LB -- "Forwards Traffic" --> CONTAINER
    
    CONTAINER -- "POST Event" --> CERE_SDK
    CONTAINER -- "GET /setWebhook" --> TELEGRAM_API

    MONITORING -- "GET /health" --> LB

    style CONTAINER fill:#D5E8D4,stroke:#82B366,stroke-width:2px
    style CERE_SDK fill:#DAE8FC,stroke:#6C8EBF,stroke-width:2px
    style TELEGRAM_API fill:#E1D5E7,stroke:#9673A6,stroke-width:2px
```

## Explanation of Deployment

1.  **Containerization**: The application is packaged as a lightweight, secure Docker container based on the `node:20-alpine` image. The `Dockerfile` uses a multi-stage build to create a small production image containing only the compiled JavaScript, `node_modules`, and other necessary assets.

2.  **Security**: The container is configured to run the application process as a **non-root user** (`nestjs`), which is a critical security best practice that limits the potential impact of a container compromise.

3.  **Deployment Environment**: The container is designed to be deployed in a container orchestration environment like Amazon ECS (Elastic Container Service) or EKS (Elastic Kubernetes Service).

4.  **Load Balancing and Scaling**:
    *   A **Load Balancer** (like an AWS Application Load Balancer) sits in front of the container(s). It terminates TLS and forwards traffic to the bot on port 8080.
    *   The application is stateless, meaning it doesn't store any session data locally. This allows it to be **horizontally scaled** by running multiple instances of the container. The load balancer would distribute incoming webhook traffic across these instances.

5.  **Network Flow**:
    *   **Inbound**: The Telegram API sends POST requests to the load balancer's public URL, which then routes them to the container's `/telegram/webhook` endpoint. The monitoring service sends GET requests to the `/health` endpoint.
    *   **Outbound**: The container makes outbound HTTPS requests to the Telegram API (for webhook registration) and the Cere Activity SDK API (for event forwarding).

6.  **Health and Monitoring**: The container exposes health check endpoints (`/health`, `/health/ready`, `/health/live`) that the orchestrator (ECS/Kubernetes) and external monitoring services use to ensure the application is running correctly and to manage its lifecycle (e.g., restarting it if it becomes unhealthy).
