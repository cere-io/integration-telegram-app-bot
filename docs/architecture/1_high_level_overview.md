# High-Level System Overview

This document provides a high-level, C4-style context diagram that illustrates the position of the **Integration Telegram App Bot** within its operational ecosystem. It shows the key external systems it interacts with and the primary data flows between them.

## Context Diagram

```mermaid
graph TD
    subgraph "Telegram Platform"
        direction LR
        TG_API[Telegram API]
        TG_USER[Telegram User]
    end

    subgraph "Cere Network"
        direction LR
        CERE_SDK[Activity SDK API]
    end

    subgraph "DevOps & Monitoring"
        direction LR
        CI_CD["CI/CD Pipeline<br>(GitHub Actions)"]
        ECR["AWS ECR<br>(Docker Registry)"]
        MONITORING["Monitoring System<br>(e.g., Prometheus, Grafana)"]
    end

    BOT["integration-telegram-app-bot-ts<br><br><b>Listens for messages, transforms them,<br>and forwards to Cere Network.</b>"]

    TG_USER --> TG_API["Sends Message"]
    TG_API -- "1. Forwards Update via Webhook" --> BOT
    BOT -- "2. Registers Webhook" --> TG_API
    BOT -- "3. Forwards Structured Event" --> CERE_SDK
    BOT -- "5. Exposes Health & Metrics" --> MONITORING
    CI_CD -- "4. Builds & Pushes Image" --> ECR

    style BOT fill:#D5E8D4,stroke:#82B366,stroke-width:2px
    style CERE_SDK fill:#DAE8FC,stroke:#6C8EBF,stroke-width:2px
    style TG_API fill:#E1D5E7,stroke:#9673A6,stroke-width:2px
```

## Explanation of Interactions

1.  **Telegram Webhook (Incoming)**: The primary input to the system. The Telegram API sends a POST request (a webhook) to the bot's `/telegram/webhook` endpoint every time a relevant event (like a new message) occurs in a configured group. This is an asynchronous, push-based interaction.

2.  **Webhook Registration (Outgoing)**: On startup, the bot makes a single API call to the Telegram API (`/setWebhook`) to register its public URL. This tells Telegram where to send future updates. This ensures the connection is always active and correctly configured after any deployment.

3.  **Event Forwarding (Outgoing)**: This is the bot's main output. After receiving and processing a Telegram message, the bot constructs a standardized event payload and sends it to the Cere Activity SDK's API endpoint. This is a critical integration that bridges the two systems.

4.  **CI/CD Pipeline (Deployment)**: The GitHub Actions workflow automatically builds a new Docker image from the `Dockerfile` on every push to the main branches. This image is then pushed to the Amazon ECR (Elastic Container Registry), making it available for deployment.

5.  **Monitoring & Health Checks (Incoming)**: The bot exposes several endpoints (e.g., `/health`, `/health/ready`, `/health/live`). External monitoring systems periodically call these endpoints to ensure the bot is running correctly and is ready to receive traffic. This is essential for maintaining service reliability in a production environment.
