# Security Architecture

This document outlines the key security measures and principles applied to the **Integration Telegram App Bot** to ensure its integrity, confidentiality, and availability.

## Security Diagram

```mermaid
graph TD
    subgraph "External Systems"
        TELEGRAM[Telegram API]
        USER[End User]
    end

    subgraph "Application Security Layers"
        direction TB
        
        L1[<b>Layer 1: Transport Layer Security</b><br>TLS 1.2+ for all external communication]
        L2[<b>Layer 2: Webhook Authentication</b><br>Validates X-Telegram-Bot-Api-Secret-Token header]
        L3[<b>Layer 3: Input Validation (DTOs)</b><br>class-validator and class-transformer<br>prevents injection and malformed data]
        L4[<b>Layer 4: Configuration Security</b><br>Securely loads secrets from<br>environment variables via ConfigModule]
        L5[<b>Layer 5: Container Security</b><br>Runs as non-root user<br>Minimal base image (node:20-alpine)]
        L6[<b>Layer 6: Cryptographic Security</b><br>Deterministic user ID generation<br>No storage of PII]
    end

    subgraph "Backend Systems"
        CERE_SDK[Cere Activity SDK]
    end

    USER -- "HTTPS" --> TELEGRAM
    TELEGRAM -- "HTTPS Webhook" --> L1
    L1 --> L2 --> L3 --> L4 --> L5 --> L6
    L6 -- "HTTPS API Call" --> CERE_SDK

    style L1 fill:#FFF9C4,stroke:#F57F17,stroke-width:2px
    style L2 fill:#FFECB3,stroke:#FFA000,stroke-width:2px
    style L3 fill:#FFE0B2,stroke:#F57C00,stroke-width:2px
    style L4 fill:#FFCCBC,stroke:#E64A19,stroke-width:2px
    style L5 fill:#FFCDD2,stroke:#D32F2F,stroke-width:2px
    style L6 fill:#F8BBD0,stroke:#C2185B,stroke-width:2px
```

## Layers of Security

1.  **Transport Layer Security**: All communication between the bot and external services (Telegram API, Cere Activity SDK) is conducted over HTTPS (TLS 1.2 or higher). This ensures that data is encrypted in transit, preventing eavesdropping or man-in-the-middle attacks.

2.  **Webhook Authentication**: The bot uses the `X-Telegram-Bot-Api-Secret-Token` header to verify that incoming webhook requests are genuinely from Telegram. The `WebhookController` immediately rejects any request that does not contain the correct, matching secret token, preventing unauthorized POST requests to the webhook endpoint.

3.  **Input Validation**: Before any processing occurs, all incoming data is rigorously validated against Data Transfer Objects (DTOs) using the `class-validator` and `class-transformer` libraries. This is a critical security measure that:
    *   Prevents NoSQL injection or other payload-based attacks.
    *   Ensures data integrity throughout the application.
    *   Rejects malformed or unexpected data at the entry point.

4.  **Secure Configuration Management**: All sensitive information, such as the `TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_TOKEN`, and Cere event signing keys, are managed as environment variables. They are loaded securely by the NestJS `ConfigModule` and are never hardcoded in the source code. This aligns with the twelve-factor app methodology.

5.  **Container Security**:
    *   **Non-Root User**: The Docker container is configured to run the application process as a limited-privilege, non-root user (`nestjs`). This significantly reduces the "blast radius" if the application process were to be compromised.
    *   **Minimal Base Image**: The use of `node:20-alpine` as a base image reduces the container's attack surface by including only the essential system libraries needed to run the application.

6.  **Cryptographic and Data Privacy**:
    *   **Deterministic Anonymization**: The `CryptoService` generates a unique, deterministic `accountId` for each Telegram user. This allows for user activity tracking without storing or processing any Personally Identifiable Information (PII) like the Telegram user ID directly in the event payload that is sent to Cere.
    *   **No PII Storage**: The bot is stateless and does not store any user data or PII at rest.
