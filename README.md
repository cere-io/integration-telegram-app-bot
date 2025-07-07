# Integration Telegram App Bot (TypeScript)

TypeScript implementation of Telegram bot that processes group messages and forwards structured events to Cere Network's Activity SDK.

## Project Status

✅ **Phase 1 Complete**: Project Setup and Foundation
- Node.js project initialized with TypeScript and NestJS
- All dependencies configured
- Project structure established
- Build and development scripts configured

✅ **Phase 2 Complete**: Core Services Infrastructure
- Configuration management system implemented
- Base service interfaces and infrastructure created
- HTTP client infrastructure with Axios setup
- Logging and error handling foundations established
- Dependency injection patterns configured

✅ **Phase 3 Complete**: Telegram Bot Integration
- Telegram Bot API service implemented with webhook setup
- Webhook controller with authentication and message filtering
- Telegram DTOs with comprehensive validation
- Webhook registration service with automatic startup
- Complete module integration with all services

✅ **Phase 4 Complete**: Message Processing Engine
- Crypto service with exact Kotlin Random compatibility (Ed25519 key generation)
- Message handler service with complete event creation and transformation
- Event DTOs for Activity SDK integration with proper validation
- Extended Telegram DTOs supporting photos, videos, and complete message data
- Human-readable message formatting and content extraction
- Kotlin compatibility validation with 100% structural matching

✅ **Phase 5 Complete**: Unified SDK Integration
- Unified SDK locally packaged with all dependencies installed
- Configuration interfaces with comprehensive DDC and Activity SDK settings
- Unified SDK service with initialization, event sending, and retry logic
- Message handler updated to use Unified SDK for event processing
- Health check service with system monitoring for all components
- Health check controller with REST endpoints for production monitoring
- Complete configuration management for DDC, Activity SDK, and processing options
- Metadata-driven routing between DDC and Activity SDK backends

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

This bot uses webhook to retrieve updates from Telegram, so you need to expose your local server to the internet.
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

### 6. Verify Setup

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

### 5. Optional: Configure Mini App

Register Mini App for bot (if needed):
- Go to Bot Settings in @BotFather
- Press 'Configure Mini App' button
- Provide information about app (you can serve app from the same URL as a bot's one - just copy static resources into `src/main/resources/META-INF/resources` folder)
- Your app will be available by direct URL `https://t.me/<bot>/<app>`
- Optionally you can configure `Menu Button` for bot to open Mini App by URL above
- Your app will be available by direct URL `https://t.me/<bot>/<app>`
- Optionally configure `Menu Button` for bot to open Mini App

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

## Development Workflow

This project follows a phased implementation approach:

1. ✅ **Phase 1**: Project Setup and Foundation (COMPLETE)
2. ✅ **Phase 2**: Core Services Infrastructure (COMPLETE)
3. ✅ **Phase 3**: Telegram Bot Integration (COMPLETE)
4. ✅ **Phase 4**: Message Processing Engine (COMPLETE)
5. ✅ **Phase 5**: Unified SDK Integration (COMPLETE)
6. **Phase 6**: Testing
7. **Phase 7**: Deployment

## Next Steps

✅ **Phase 6 Complete**: Testing and Validation
- Comprehensive test suite implemented
- Integration and E2E tests validated
- Compatibility with Kotlin implementation confirmed

🚀 **Phase 7 In Progress**: Deployment and Production
- Dockerization of the application is currently underway.

## License

MIT 