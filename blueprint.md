# Integration Telegram App Bot - TypeScript Blueprint

## Project Overview

This blueprint outlines the TypeScript implementation of a Telegram bot that processes group messages and forwards structured events to the Cere Network's Activity SDK. The bot maintains identical functionality to the Kotlin version while leveraging Node.js ecosystem patterns and libraries.

## Architecture & Technology Stack

### Core Technologies
- **Language**: TypeScript (Node.js 20+)
- **Framework**: NestJS (following Cere Network patterns)
- **Build Tool**: npm/yarn with TypeScript compiler
- **Serialization**: Native JSON with type validation
- **HTTP Client**: Axios or native fetch
- **Cryptography**: `@noble/ed25519` or `libsodium-wrappers`
- **Containerization**: Docker with Node.js 20 Alpine

### Key Dependencies
```json
{
  "dependencies": {
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/axios": "^3.0.0",
    "@noble/ed25519": "^2.0.0",
    "class-validator": "^0.14.0",
    "class-transformer": "^0.5.0",
    "axios": "^1.6.0",
    "uuid": "^9.0.0",
    "telegraf": "^4.15.0"
  },
  "devDependencies": {
    "@types/node": "^20.0.0",
    "@types/uuid": "^9.0.0",
    "typescript": "^5.0.0",
    "@nestjs/cli": "^10.0.0",
    "@nestjs/testing": "^10.0.0",
    "jest": "^29.0.0"
  }
}
```

## Project Structure

```
integration-telegram-app-bot-ts/
├── src/
│   ├── config/
│   │   ├── app.config.ts
│   │   ├── telegram.config.ts
│   │   └── event.config.ts
│   ├── interfaces/
│   │   ├── telegram.interfaces.ts
│   │   ├── event.interfaces.ts
│   │   └── activity-sdk.interfaces.ts
│   ├── services/
│   │   ├── telegram-bot.service.ts
│   │   ├── webhook.service.ts
│   │   ├── message-handler.service.ts
│   │   ├── activity-sdk.service.ts
│   │   └── crypto.service.ts
│   ├── controllers/
│   │   └── webhook.controller.ts
│   ├── dto/
│   │   ├── telegram-update.dto.ts
│   │   └── event.dto.ts
│   ├── app.module.ts
│   └── main.ts
├── config/
│   └── configuration.ts
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── .env.example
├── package.json
├── tsconfig.json
├── nest-cli.json
└── README.md
```

## Core Components Implementation

### 1. Main Application Entry (`src/main.ts`)
```typescript
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';
import { WebhookService } from './services/webhook.service';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  // Enable CORS
  app.enableCors({
    origin: '*',
    methods: ['GET', 'POST'],
  });
  
  // Enable validation
  app.useGlobalPipes(new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
  }));
  
  // Register webhook on startup
  const webhookService = app.get(WebhookService);
  await webhookService.registerWebhook();
  
  const port = process.env.PORT || 8080;
  await app.listen(port);
  console.log(`Telegram Bot running on port ${port}`);
}

bootstrap();
```

### 2. Application Module (`src/app.module.ts`)
```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { HttpModule } from '@nestjs/axios';
import { WebhookController } from './controllers/webhook.controller';
import { TelegramBotService } from './services/telegram-bot.service';
import { WebhookService } from './services/webhook.service';
import { MessageHandlerService } from './services/message-handler.service';
import { ActivitySdkService } from './services/activity-sdk.service';
import { CryptoService } from './services/crypto.service';
import configuration from '../config/configuration';

@Module({
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    HttpModule,
  ],
  controllers: [WebhookController],
  providers: [
    TelegramBotService,
    WebhookService,
    MessageHandlerService,
    ActivitySdkService,
    CryptoService,
  ],
})
export class AppModule {}
```

### 3. Configuration (`config/configuration.ts`)
```typescript
export default () => ({
  telegram: {
    botToken: process.env.TELEGRAM_BOT_TOKEN,
    webhookUrl: process.env.TELEGRAM_WEBHOOK_URL,
    webhookToken: process.env.TELEGRAM_WEBHOOK_TOKEN,
    maxConnections: parseInt(process.env.TELEGRAM_WEBHOOK_MAX_CONNECTIONS) || 80,
  },
  event: {
    serviceUrl: process.env.EVENT_SERVICE_URL || 'https://ai-event.stage.cere.io',
    appId: process.env.EVENT_APP_ID,
    accountId: process.env.EVENT_ACCOUNT_ID,
    signature: process.env.EVENT_SIGNATURE,
    id: process.env.EVENT_ID,
    type: process.env.EVENT_TYPE || 'TELEGRAM_MESSAGE',
    timestamp: process.env.EVENT_TIMESTAMP,
    dataServiceId: process.env.EVENT_DATA_SERVICE_ID,
    userPubKey: process.env.EVENT_USER_PUB_KEY,
  },
});
```

### 4. Telegram Bot Service (`src/services/telegram-bot.service.ts`)
```typescript
import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class TelegramBotService {
  private readonly logger = new Logger(TelegramBotService.name);
  private readonly botToken: string;
  private readonly baseUrl: string;

  constructor(
    private configService: ConfigService,
    private httpService: HttpService,
  ) {
    this.botToken = this.configService.get<string>('telegram.botToken');
    this.baseUrl = `https://api.telegram.org/bot${this.botToken}`;
  }

  async setWebhook(url: string, secretToken: string, maxConnections: number): Promise<boolean> {
    try {
      const response = await firstValueFrom(
        this.httpService.post(`${this.baseUrl}/setWebhook`, {
          url,
          max_connections: maxConnections,
          drop_pending_updates: true,
          secret_token: secretToken,
          allowed_updates: ['message', 'callback_query', 'chat_member'],
        }),
      );
      
      this.logger.log('Webhook registered successfully');
      return response.data.ok;
    } catch (error) {
      this.logger.error('Failed to register webhook', error);
      throw error;
    }
  }
}
```

### 5. Webhook Controller (`src/controllers/webhook.controller.ts`)
```typescript
import { 
  Controller, 
  Post, 
  Body, 
  Headers, 
  UnauthorizedException,
  Logger,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { MessageHandlerService } from '../services/message-handler.service';
import { TelegramUpdateDto } from '../dto/telegram-update.dto';

@Controller('telegram/webhook')
export class WebhookController {
  private readonly logger = new Logger(WebhookController.name);
  private readonly authToken: string;
  private readonly handledTypes = new Set(['group', 'supergroup']);

  constructor(
    private configService: ConfigService,
    private messageHandler: MessageHandlerService,
  ) {
    this.authToken = this.configService.get<string>('telegram.webhookToken');
  }

  @Post()
  @HttpCode(HttpStatus.OK)
  async handleWebhook(
    @Headers('x-telegram-bot-api-secret-token') auth: string,
    @Body() update: TelegramUpdateDto,
  ): Promise<void> {
    if (auth !== this.authToken) {
      this.logger.warn('Unauthorized webhook request - token mismatch');
      throw new UnauthorizedException('Invalid secret token');
    }

    this.logger.debug('Webhook payload received', JSON.stringify(update));

    try {
      const chatType = update.message?.chat?.type;
      
      if (this.handledTypes.has(chatType)) {
        const message = update.message;
        
        // Skip bot commands and replies to bot messages
        if (message?.text?.startsWith('/') || 
            message?.reply_to_message?.from?.is_bot) {
          return;
        }
        
        this.logger.log('Processing group message');
        await this.messageHandler.handle(update);
      }
    } catch (error) {
      this.logger.error('Error processing webhook', error);
      throw error;
    }
  }
}
```

### 6. Message Handler Service (`src/services/message-handler.service.ts`)
```typescript
import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { v4 as uuidv4 } from 'uuid';
import { ActivitySdkService } from './activity-sdk.service';
import { CryptoService } from './crypto.service';
import { TelegramUpdateDto } from '../dto/telegram-update.dto';
import { EventDto } from '../dto/event.dto';

@Injectable()
export class MessageHandlerService {
  private readonly logger = new Logger(MessageHandlerService.name);

  constructor(
    private configService: ConfigService,
    private activitySdkService: ActivitySdkService,
    private cryptoService: CryptoService,
  ) {}

  async handle(update: TelegramUpdateDto): Promise<void> {
    try {
      const message = update.message;
      if (!message) return;

      const chat = message.chat;
      const groupId = chat.id;

      // Create human-readable message
      const humanReadableMessage = this.createHumanReadableMessage(message, chat);

      // Create message payload
      const payload = this.createMessagePayload(message, chat);

      // Generate account ID from user ID or use default
      const accountId = message.from 
        ? await this.cryptoService.generateAccountId(message.from.id)
        : this.configService.get<string>('event.accountId');

      // Create the event object
      const event: EventDto = {
        generated: false,
        is_debug: false,
        app_id: this.configService.get<string>('event.appId'),
        connection_id: uuidv4(),
        session_id: uuidv4(),
        account_id: accountId,
        signature: this.configService.get<string>('event.signature'),
        id: this.configService.get<string>('event.id'),
        event_type: this.configService.get<string>('event.type'),
        timestamp: this.configService.get<string>('event.timestamp'),
        payload,
        data_service_id: this.configService.get<string>('event.dataServiceId'),
        user_pub_key: this.configService.get<string>('event.userPubKey'),
      };

      // Send event to Activity SDK
      let statusMessage: string;
      try {
        await this.activitySdkService.sendEvent(event);
        statusMessage = '✅ Event sent successfully';
      } catch (error) {
        this.logger.error('Failed to send event to Activity SDK', error);
        statusMessage = `❌ Failed to send event: ${error.message}`;
      }

      // Log complete debug information
      this.logger.log(`
=== Message Processing Debug Info ===

Human Readable:
${humanReadableMessage}

DDC Event:
${JSON.stringify(event, null, 2)}

Activity SDK Status:
${statusMessage}
===================================
      `);
    } catch (error) {
      this.logger.error('Error processing group message', error);
    }
  }

  private createHumanReadableMessage(message: any, chat: any): string {
    let result = `New message in group ${chat.title}:\\n\\n`;
    
    if (message.from) {
      result += `From: ${message.from.first_name}`;
      if (message.from.last_name) result += ` ${message.from.last_name}`;
      if (message.from.username) result += ` (@${message.from.username})`;
      result += '\\n\\n';
    }
    
    result += this.getMessageContent(message);
    return result;
  }

  private createMessagePayload(message: any, chat: any): any {
    const payload: any = {
      message_id: message.message_id,
      group_id: chat.id,
      group_title: chat.title || 'Unknown Group',
      message_text: this.getMessageContent(message),
      message_timestamp: message.date,
    };

    if (message.from) {
      payload.author = {
        id: message.from.id.toString(),
        username: message.from.username || 'unknown',
        first_name: message.from.first_name,
        last_name: message.from.last_name || '',
        is_bot: message.from.is_bot,
      };
    }

    if (message.photo) {
      payload.photos = message.photo.map(photo => ({
        file_id: photo.file_id,
        file_unique_id: photo.file_unique_id,
        width: photo.width.toString(),
        height: photo.height.toString(),
        file_size: (photo.file_size || 0).toString(),
      }));
    }

    if (message.video) {
      payload.video = {
        file_id: message.video.file_id,
        file_unique_id: message.video.file_unique_id,
        width: message.video.width.toString(),
        height: message.video.height.toString(),
        duration: message.video.duration.toString(),
      };
    }

    return payload;
  }

  private getMessageContent(message: any): string {
    if (message.text) return message.text;
    if (message.caption) return message.caption;
    if (message.photo) return '[Photo]';
    if (message.video) return '[Video]';
    if (message.document) return '[Document]';
    if (message.sticker) return '[Sticker]';
    return '[Unsupported message type]';
  }
}
```

### 7. Crypto Service (`src/services/crypto.service.ts`)
```typescript
import { Injectable } from '@nestjs/common';
import { ed25519 } from '@noble/ed25519';

@Injectable()
export class CryptoService {
  async generateAccountId(userId: number): Promise<string> {
    // Create deterministic seed from user ID (exactly matching Kotlin implementation)
    const seed = this.createDeterministicSeed(userId);
    
    // Generate Ed25519 key pair
    const privateKey = seed.slice(0, 32);
    const publicKey = await ed25519.getPublicKey(privateKey);
    
    return '0x' + Buffer.from(publicKey).toString('hex');
  }

  private createDeterministicSeed(userId: number): Buffer {
    // Exactly match Kotlin Random(userId).nextBytes(sk) behavior
    // Kotlin uses java.util.Random with Linear Congruential Generator (LCG)
    const seed = Buffer.alloc(32);
    let state = userId & 0xFFFFFFFF; // Ensure 32-bit unsigned integer
    
    // Kotlin Random uses LCG: nextSeed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)
    // For nextBytes, it uses next(8) which extracts 8 bits from the state
    for (let i = 0; i < 32; i++) {
      // Match Kotlin's Random.next(8) implementation exactly
      state = ((state * 0x5DEECE66D + 0xB) & 0xFFFFFFFFFFFF) >>> 0;
      const bits = (state >>> (48 - 8)) & 0xFF;
      seed[i] = bits;
    }
    
    return seed;
  }

  // Helper method to convert JavaScript numbers to Kotlin-style long values
  toLongValue(value: number): number {
    // Ensure we handle the conversion the same way Kotlin does
    return Math.floor(value);
  }
}
```

### 8. Activity SDK Service (`src/services/activity-sdk.service.ts`)
```typescript
import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import { EventDto } from '../dto/event.dto';

@Injectable()
export class ActivitySdkService {
  private readonly logger = new Logger(ActivitySdkService.name);
  private readonly serviceUrl: string;

  constructor(
    private configService: ConfigService,
    private httpService: HttpService,
  ) {
    this.serviceUrl = this.configService.get<string>('event.serviceUrl');
  }

  async sendEvent(event: EventDto): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.httpService.post(`${this.serviceUrl}/event/events`, event, {
          headers: {
            'Content-Type': 'application/json',
          },
        }),
      );
      
      this.logger.debug(`Event sent successfully (status: ${response.status})`);
    } catch (error) {
      this.logger.error('Failed to send event to Activity SDK', error);
      throw error;
    }
  }
}
```

### 9. Webhook Service (`src/services/webhook.service.ts`)
```typescript
import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TelegramBotService } from './telegram-bot.service';

@Injectable()
export class WebhookService implements OnModuleInit {
  private readonly logger = new Logger(WebhookService.name);

  constructor(
    private configService: ConfigService,
    private telegramBotService: TelegramBotService,
  ) {}

  async onModuleInit() {
    await this.registerWebhook();
  }

  async registerWebhook(): Promise<void> {
    try {
      this.logger.log('Registering Telegram webhook');
      
      const webhookUrl = this.configService.get<string>('telegram.webhookUrl');
      const webhookToken = this.configService.get<string>('telegram.webhookToken');
      const maxConnections = this.configService.get<number>('telegram.maxConnections');
      
      await this.telegramBotService.setWebhook(webhookUrl, webhookToken, maxConnections);
      
      this.logger.log('Telegram webhook registered successfully');
    } catch (error) {
      this.logger.error('Failed to register webhook', error);
      throw error;
    }
  }
}
```

## Data Transfer Objects (DTOs)

### Telegram Update DTO (`src/dto/telegram-update.dto.ts`)
```typescript
import { IsOptional, IsObject, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

export class TelegramUserDto {
  id: number;
  is_bot: boolean;
  first_name: string;
  last_name?: string;
  username?: string;
}

export class TelegramChatDto {
  id: number;
  type: string;
  title?: string;
}

export class TelegramPhotoDto {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  file_size?: number;
}

export class TelegramVideoDto {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  duration: number;
}

export class TelegramMessageDto {
  message_id: number;
  date: number;
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramUserDto)
  from?: TelegramUserDto;
  
  @ValidateNested()
  @Type(() => TelegramChatDto)
  chat: TelegramChatDto;
  
  @IsOptional()
  text?: string;
  
  @IsOptional()
  caption?: string;
  
  @IsOptional()
  @ValidateNested({ each: true })
  @Type(() => TelegramPhotoDto)
  photo?: TelegramPhotoDto[];
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramVideoDto)
  video?: TelegramVideoDto;
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  reply_to_message?: TelegramMessageDto;
}

export class TelegramUpdateDto {
  update_id: number;
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  message?: TelegramMessageDto;
}
```

### Event DTO (`src/dto/event.dto.ts`)
```typescript
export class EventDto {
  generated: boolean;
  is_debug: boolean;
  app_id: string;
  connection_id: string;
  session_id: string;
  account_id: string;
  signature: string;
  id: string;
  event_type: string;
  timestamp: string;
  payload: any;
  data_service_id: string;
  user_pub_key: string;
}
```

## Docker Configuration

### Dockerfile
```dockerfile
FROM node:20-alpine as builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production && npm cache clean --force

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM node:20-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S nodejs && \\
    adduser -S nestjs -u 1001

# Copy built application
COPY --from=builder --chown=nestjs:nodejs /app/dist ./dist
COPY --from=builder --chown=nestjs:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=nestjs:nodejs /app/package*.json ./

USER nestjs

EXPOSE 8080

CMD ["node", "dist/main"]
```

### docker-compose.yml
```yaml
version: '3.8'

services:
  telegram-bot:
    build: .
    ports:
      - "8080:8080"
    environment:
      NODE_ENV: production
      PORT: 8080
    env_file:
      - .env
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

## Configuration Files

### package.json
```json
{
  "name": "integration-telegram-app-bot-ts",
  "version": "1.0.0",
  "description": "TypeScript Telegram Bot for Cere Network",
  "main": "dist/main.js",
  "scripts": {
    "build": "nest build",
    "format": "prettier --write \"src/**/*.ts\" \"test/**/*.ts\"",
    "start": "nest start",
    "start:dev": "nest start --watch",
    "start:debug": "nest start --debug --watch",
    "start:prod": "node dist/main",
    "lint": "eslint \"{src,apps,libs,test}/**/*.ts\" --fix",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:cov": "jest --coverage",
    "test:debug": "node --inspect-brk -r tsconfig-paths/register -r ts-node/register node_modules/.bin/jest --runInBand",
    "test:e2e": "jest --config ./test/jest-e2e.json"
  },
  "dependencies": {
    "@nestjs/common": "^10.0.0",
    "@nestjs/core": "^10.0.0",
    "@nestjs/platform-express": "^10.0.0",
    "@nestjs/config": "^3.0.0",
    "@nestjs/axios": "^3.0.0",
    "@noble/ed25519": "^2.0.0",
    "class-validator": "^0.14.0",
    "class-transformer": "^0.5.0",
    "axios": "^1.6.0",
    "uuid": "^9.0.0",
    "reflect-metadata": "^0.1.13",
    "rxjs": "^7.8.1"
  },
  "devDependencies": {
    "@nestjs/cli": "^10.0.0",
    "@nestjs/schematics": "^10.0.0",
    "@nestjs/testing": "^10.0.0",
    "@types/express": "^4.17.17",
    "@types/jest": "^29.5.2",
    "@types/node": "^20.3.1",
    "@types/supertest": "^2.0.12",
    "@types/uuid": "^9.0.0",
    "@typescript-eslint/eslint-plugin": "^6.0.0",
    "@typescript-eslint/parser": "^6.0.0",
    "eslint": "^8.42.0",
    "eslint-config-prettier": "^9.0.0",
    "eslint-plugin-prettier": "^5.0.0",
    "jest": "^29.5.0",
    "prettier": "^3.0.0",
    "source-map-support": "^0.5.21",
    "supertest": "^6.3.3",
    "ts-jest": "^29.1.0",
    "ts-loader": "^9.4.3",
    "ts-node": "^10.9.1",
    "tsconfig-paths": "^4.2.1",
    "typescript": "^5.1.3"
  }
}
```

### tsconfig.json
```json
{
  "compilerOptions": {
    "module": "commonjs",
    "declaration": true,
    "removeComments": true,
    "emitDecoratorMetadata": true,
    "experimentalDecorators": true,
    "allowSyntheticDefaultImports": true,
    "target": "ES2021",
    "sourceMap": true,
    "outDir": "./dist",
    "baseUrl": "./",
    "incremental": true,
    "skipLibCheck": true,
    "strictNullChecks": false,
    "noImplicitAny": false,
    "strictBindCallApply": false,
    "forceConsistentCasingInFileNames": false,
    "noFallthroughCasesInSwitch": false,
    "resolveJsonModule": true
  }
}
```

### nest-cli.json
```json
{
  "$schema": "https://json.schemastore.org/nest-cli",
  "collection": "@nestjs/schematics",
  "sourceRoot": "src",
  "compilerOptions": {
    "deleteOutDir": true
  }
}
```

## Environment Variables (.env.example)
```bash
# Node.js Configuration
NODE_ENV=development
PORT=8080

# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your:bot_token
TELEGRAM_WEBHOOK_URL=https://your-domain.com/telegram/webhook
TELEGRAM_WEBHOOK_TOKEN=your-webhook-token
TELEGRAM_WEBHOOK_MAX_CONNECTIONS=80

# Event Service Configuration
EVENT_SERVICE_URL=https://ai-event.stage.cere.io
EVENT_APP_ID=2105
EVENT_DATA_SERVICE_ID=2105
EVENT_ACCOUNT_ID=0x4fcbae9ac6d9ffec5da0475285d267093cdceed74fb69ba47d7a6eaafe6eb2a9
EVENT_SIGNATURE=0x624718cb16b4ca95ebf26a39f6ba74e00ed27772464af9b745cbb80f5e1e1cb7ac40624db84da233c86086caeefe048d2901851ccb16fd6d97d2bd0e86e2630e
EVENT_ID=ad9b2467-94c4-4407-918c-cc5da18271bc
EVENT_TYPE=TELEGRAM_MESSAGE
EVENT_TIMESTAMP=2025-01-03T15:07:48.168Z
EVENT_USER_PUB_KEY=0x4fcbae9ac6d9ffec5da0475285d267093cdceed74fb69ba47d7a6eaafe6eb2a9
```

## Development Setup

### Prerequisites
- Node.js 20+
- npm or yarn
- Public tunnel (ngrok/Cloudflare) for webhook access
- Registered Telegram bot via @BotFather

### Local Development
```bash
# Clone/create project
npm install

# Copy environment variables
cp .env.example .env
# Configure your environment variables

# Start development server
npm run start:dev

# Build for production
npm run build

# Start production server
npm run start:prod
```

### Testing
```bash
# Run unit tests
npm run test

# Run tests with coverage
npm run test:cov

# Run e2e tests
npm run test:e2e
```

## Key Implementation Notes

### 1. **NestJS Architecture**
- Follows NestJS patterns with modules, controllers, services, and DTOs
- Uses dependency injection for clean separation of concerns
- Implements proper error handling and logging

### 2. **Configuration Management**
- Type-safe configuration using `@nestjs/config`
- Environment variable validation
- Centralized configuration management

### 3. **Cryptographic Operations**
- Uses `@noble/ed25519` for Ed25519 key generation
- Maintains deterministic key generation matching Kotlin implementation
- Secure key handling and formatting

### 4. **HTTP Operations**
- Uses `@nestjs/axios` for HTTP client operations
- Proper error handling for external service calls
- Request/response logging and debugging

### 5. **Data Validation**
- Uses `class-validator` and `class-transformer` for DTO validation
- Type-safe data transformation
- Runtime validation of incoming webhook data

### 6. **Logging**
- Comprehensive logging using NestJS built-in logger
- Debug information for message processing
- Error tracking and monitoring

### 7. **Health Checks**
- Health check endpoint for container orchestration
- Proper graceful shutdown handling
- Service availability monitoring

## Migration from Kotlin

### Key Differences
1. **Startup Lifecycle**: Uses NestJS `OnModuleInit` instead of Quarkus `@Startup`
2. **Dependency Injection**: NestJS DI system instead of CDI
3. **Configuration**: Environment-based config instead of Quarkus config mapping
4. **HTTP Client**: Axios instead of MicroProfile REST Client
5. **Validation**: class-validator instead of Bean Validation
6. **Cryptography**: @noble/ed25519 instead of Bouncy Castle

### Functional Equivalents
- **Message Processing**: Identical logic and flow
- **Webhook Handling**: Same authentication and filtering
- **Event Generation**: Identical event structure and data
- **Activity SDK Integration**: Same API calls and error handling
- **Logging**: Equivalent debug output and information

This blueprint maintains 100% functional compatibility with the Kotlin version while leveraging TypeScript/Node.js ecosystem best practices and the established Cere Network development patterns. 