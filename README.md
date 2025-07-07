# Integration Telegram App Bot (TypeScript)

## Project Overview and Migration Analysis

This project is a complete, production-ready TypeScript implementation of the Cere Telegram App Bot, designed to replace the original Kotlin-based bot. It replicates all core functionalities of the original while introducing significant architectural improvements, enhanced security, and a comprehensive testing suite.

### Feature Parity and Enhancement Summary

| Feature | Original Kotlin Bot | New TypeScript Bot | Analysis |
| :--- | :--- | :--- | :--- |
| **Framework** | Quarkus | NestJS | **Parity.** Modern, robust frameworks. |
| **Configuration** | `application.yml` | Type-safe, environment-aware | **Enhancement.** More maintainable and less error-prone. |
| **Dependencies** | Gradle / Maven | npm / Yarn | **Parity.** Modern, equivalent libraries for all functionalities. |
| **Webhook Logic** | Basic group messages | Handles multiple update types | **Enhancement.** More resilient and extensible for future features. |
| **Cryptography** | Bouncy Castle (Ed25519) | `@noble/ed25519` | **Parity.** 100% deterministic key generation compatibility is maintained. A risky cryptographic fallback in the TS bot has been removed to guarantee consistency. |
| **Event Dispatch** | Direct REST Client | `UnifiedSdkService` | **Major Enhancement.** Centralizes event sending logic, adding robustness with built-in error handling and retry mechanisms. |
| **Health Checks** | Basic | Comprehensive REST endpoints | **Major Enhancement.** Production-ready monitoring for system health, readiness, and liveness. |
| **Testing** | No automated tests | Comprehensive Jest test suite | **Major Enhancement.** Ensures reliability, prevents regressions, and supports future development. |
| **Deployment** | Docker | Secure, non-root Docker | **Enhancement.** Improved container security. |
| **Documentation** | Basic `README.md` | Extensive documentation | **Major Enhancement.** Detailed setup, architecture, and development guides. |

### Detailed Architectural Improvements

*   **Modern Stack**: Built on Node.js v20 and NestJS, a powerful and widely-adopted framework for building scalable server-side applications.
*   **Enhanced Security**: The production Docker container runs as a non-root user, a critical security best practice. The cryptographic service has been hardened by removing a risky fallback mechanism, ensuring only the correct, compatible Ed25519 implementation is used.
*   **Robust Testing**: A comprehensive test suite using Jest provides strong guarantees against regressions and simplifies future development. This is a critical advantage over the original implementation, which lacked automated tests.
*   **Unified SDK Integration**: Centralizes all communication with the Cere network through a dedicated service, making the bot's core logic cleaner and more focused.
*   **Superior Documentation**: The project includes extensive documentation, from a high-level `blueprint.md` to detailed setup and development guides, making it significantly easier to maintain and onboard new developers.

## What This Bot Does

### Core Purpose

At its heart, this bot acts as a **data bridge** between the Telegram platform and the Cere Network's backend systems.

Its primary job is to **monitor messages in specific Telegram groups**, transform them into structured, meaningful **events**, and then reliably forward these events to Cere's Activity SDK for processing.

### Key Capabilities and Workflow

Here’s a step-by-step breakdown of what the bot does:

1.  **Listens for Telegram Activity**: The bot is configured to receive real-time updates (via webhooks) from Telegram whenever a message is sent in a group it's a member of.

2.  **Processes Various Message Types**: It's not limited to just text. It understands and processes:
    *   **Text messages** and their content.
    *   **Media messages**, noting when a user sends a **photo** or **video**.
    *   **Edited messages** and channel posts.

3.  **Filters Out Noise**: To ensure only relevant data is captured, the bot intelligently ignores:
    *   Commands sent to it or other bots (e.g., `/start`).
    *   Messages that are replies to other bots.

4.  **Generates a Unique User Identity**: This is a critical function. For each Telegram user, the bot uses a deterministic cryptographic function to generate a unique and stable `accountId`. This allows the Cere network to recognize a user's activity over time without ever needing to know their actual Telegram identity, preserving privacy.

5.  **Creates a Structured Event**: It takes the raw, sometimes messy data from a Telegram message and transforms it into a clean, standardized JSON event. This event contains key information like:
    *   A unique ID for the event.
    *   The generated `accountId` of the user.
    *   The group ID and title.
    *   The message content (or a placeholder like `[Photo]`).
    *   The timestamp of the message.

6.  **Forwards the Event Reliably**: Using its internal `UnifiedSdkService`, the bot sends this structured event to the Cere Activity SDK. This service ensures that the event is dispatched correctly and includes logic for retries and error handling.

7.  **Serves a Mini App**: The bot can also host a simple web application (a "Mini App") directly within Telegram. This allows users to interact with a web interface without leaving the Telegram app.

8.  **Monitors Its Own Health**: It provides a set of API endpoints (e.g., `/health`, `/ready`) so that production monitoring systems can continuously check if the bot is running correctly and is ready to handle messages.

## Prerequisites

- Node.js 20+ installed
- npm or yarn package manager
- Git repository initialized
- IDE/Editor with TypeScript support

## Setup Instructions

### 1. Register Your Telegram Bot

Register your bot with [@BotFather](https://t.me/BotFather):
- Call `/newbot` command
- Give a name to your bot (something like `DEV Your Name's Bot`)
- Choose username for bot (something like `DEV_YourName_Bot`)
- Copy bot token and save in safe place

### 2. Configure Webhook Tunnel

This bot uses a webhook to retrieve updates from Telegram, so you need to expose your local server to the internet.
Configure one of the tunnel solutions to work with your local 8080 port:
- [ngrok](https://ngrok.com)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/)

Example with ngrok:
```bash
# Install ngrok and expose port 8080
ngrok http 8080
# Copy the HTTPS URL (e.g., https://abc123.ngrok.io)
```

### 3. Configure Environment

```bash
# Install dependencies
npm install

# Install NestJS CLI globally (if not already installed)
npm install -g @nestjs/cli

# Copy environment template
cp .env.example .env
```

Edit `.env` with your actual values:
```bash
# Required Telegram Configuration
TELEGRAM_BOT_TOKEN=<token from @BotFather>
TELEGRAM_WEBHOOK_URL=<your tunnel URL>/telegram/webhook
TELEGRAM_WEBHOOK_TOKEN=<generate a secure random token>

# Other configurations as needed...
```

### 4. Start Development Server

```bash
# Start development server
npm run start:dev
```

The application will start on port 8080 and automatically register the webhook with Telegram.

### 5. Verify Setup

Once running, you can verify the setup using the health check endpoints:
- `GET /health` - Overall system health
- `GET /health/ready` - Readiness check
- `GET /health/live` - Liveness check
- `GET /health/unified-sdk` - Unified SDK status
- `GET /health/ping` - Simple ping test

Example:
```bash
curl http://localhost:8080/health
```

### 6. Optional: Configure Mini App

Register a Mini App for your bot (if needed):
- Go to Bot Settings in @BotFather
- Press 'Configure Mini App' button
- Provide information about your app (you can serve the app from the same URL as the bot's webhook - just copy static resources into the `src/main/resources/META-INF/resources` folder)
- Your app will be available by direct URL `https://t.me/<bot_username>/<app_name>`
- Optionally, you can configure a `Menu Button` for the bot to open the Mini App.

## Project Structure

```
integration-telegram-app-bot-ts/
├── src/
│   ├── config/           # Configuration files
│   ├── interfaces/       # TypeScript interfaces
│   │   ├── config.interfaces.ts
│   │   ├── telegram.interfaces.ts
│   │   ├── event.interfaces.ts
│   │   └── unified-sdk.interfaces.ts
│   ├── services/         # Business logic services
│   │   ├── base-http.service.ts
│   │   ├── telegram-bot.service.ts
│   │   ├── webhook.service.ts
│   │   ├── message-handler.service.ts
│   │   ├── unified-sdk.service.ts
│   │   ├── health-check.service.ts
│   │   ├── crypto.service.ts
│   │   ├── logger.service.ts
│   │   ├── validation.service.ts
│   │   └── error-handler.service.ts
│   ├── controllers/      # HTTP controllers
│   │   ├── webhook.controller.ts
│   │   └── health-check.controller.ts
│   ├── dto/             # Data Transfer Objects
│   │   ├── base.dto.ts
│   │   ├── telegram-update.dto.ts
│   │   └── event.dto.ts
│   ├── validation/       # Compatibility validation
│   │   └── kotlin-compatibility.ts
│   ├── unified-sdk/      # Locally packaged Unified SDK
│   ├── app.module.ts    # Root application module
│   └── main.ts          # Application entry point
├── config/              # External configuration
│   └── configuration.ts # Configuration factory
├── docker/              # Docker configuration
├── test/                # Test files
├── docs/                # Documentation
└── ...
```

## Available Scripts

- `npm run build` - Build the application
- `npm run start` - Start the application
- `npm run start:dev` - Start in development mode with watch
- `npm run start:debug` - Start in debug mode
- `npm run start:prod` - Start in production mode
- `npm run test` - Run tests
- `npm run test:watch` - Run tests in watch mode
- `npm run test:cov` - Run tests with coverage
- `npm run lint` - Lint the code
- `npm run format` - Format the code