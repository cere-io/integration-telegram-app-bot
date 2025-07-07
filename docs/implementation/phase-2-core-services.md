# Phase 2: Core Services Infrastructure

## Objectives
- Implement configuration management system
- Create base service interfaces and infrastructure
- Setup HTTP client infrastructure with Axios
- Implement logging and error handling foundations
- Establish dependency injection patterns

## Prerequisites
- ✅ Phase 1 completed successfully
- ✅ Project structure established (`src/` directories)
- ✅ All dependencies installed via npm
- ✅ TypeScript and NestJS configuration working
- ✅ Basic application files (`main.ts`, `app.module.ts`) created

## Dependencies from Phase 1
- **Project Structure**: Using `src/config/`, `src/services/`, `src/interfaces/`
- **Package Configuration**: Leveraging installed `@nestjs/config`, `@nestjs/axios`
- **TypeScript Setup**: Building on compiler configuration
- **Environment Variables**: Extending `.env.example` structure

## Tasks

### Task 2.1: Implement Configuration Management
**Duration**: 1.5 hours  
**Description**: Create type-safe configuration system using NestJS config module

**Dependencies**: 
- Phase 1 Task 1.7 (Environment configuration template)
- Phase 1 Task 1.2 (`@nestjs/config` dependency)

Create `config/configuration.ts`:
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
  app: {
    port: parseInt(process.env.PORT) || 8080,
    nodeEnv: process.env.NODE_ENV || 'development',
  },
});
```

Create `src/interfaces/config.interfaces.ts`:
```typescript
export interface TelegramConfig {
  botToken: string;
  webhookUrl: string;
  webhookToken: string;
  maxConnections: number;
}

export interface EventConfig {
  serviceUrl: string;
  appId: string;
  accountId: string;
  signature: string;
  id: string;
  type: string;
  timestamp: string;
  dataServiceId: string;
  userPubKey: string;
}

export interface AppConfig {
  port: number;
  nodeEnv: string;
}

export interface ApplicationConfiguration {
  telegram: TelegramConfig;
  event: EventConfig;
  app: AppConfig;
}
```

**Deliverables**:
- Configuration factory function with type safety
- Configuration interfaces for all sections
- Environment variable parsing and defaults

### Task 2.2: Create Base Service Interfaces
**Duration**: 1 hour  
**Description**: Define core service interfaces and base classes

**Dependencies**: 
- Task 2.1 (Configuration interfaces)
- Phase 1 Task 1.5 (Project structure with interfaces directory)

Create `src/interfaces/telegram.interfaces.ts`:
```typescript
export interface TelegramUser {
  id: number;
  is_bot: boolean;
  first_name: string;
  last_name?: string;
  username?: string;
}

export interface TelegramChat {
  id: number;
  type: string;
  title?: string;
}

// Explicit photo structure matching Kotlin PhotoSize
export interface TelegramPhoto {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  file_size?: number;
}

// Explicit video structure matching Kotlin Video
export interface TelegramVideo {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  duration: number;
}

// Document structure for completeness
export interface TelegramDocument {
  file_id: string;
  file_unique_id: string;
  file_name?: string;
  mime_type?: string;
  file_size?: number;
}

// Sticker structure for completeness
export interface TelegramSticker {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
}

export interface TelegramMessage {
  message_id: number;
  date: number;
  from?: TelegramUser;
  chat: TelegramChat;
  text?: string;
  caption?: string;
  // Photo is ALWAYS an array in Telegram API (different sizes of same photo)
  photo?: TelegramPhoto[];
  video?: TelegramVideo;
  document?: TelegramDocument;
  sticker?: TelegramSticker;
  reply_to_message?: TelegramMessage;
}

export interface TelegramUpdate {
  update_id: number;
  message?: TelegramMessage;
}

export interface WebhookSetupRequest {
  url: string;
  max_connections: number;
  drop_pending_updates: boolean;
  secret_token: string;
  // Exactly match Kotlin allowed_updates specification
  allowed_updates: ['message', 'callback_query', 'chat_member'];
}
```

Create `src/interfaces/event.interfaces.ts`:
```typescript
export interface EventPayload {
  message_id: number;
  group_id: number;
  group_title: string;
  message_text: string;
  message_timestamp: number;
  author?: {
    id: string;
    username: string;
    first_name: string;
    last_name: string;
    is_bot: boolean;
  };
  photos?: Array<{
    file_id: string;
    file_unique_id: string;
    width: string;
    height: string;
    file_size: string;
  }>;
  video?: {
    file_id: string;
    file_unique_id: string;
    width: string;
    height: string;
    duration: string;
  };
}

export interface ActivityEvent {
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
  payload: EventPayload;
  data_service_id: string;
  user_pub_key: string;
}
```

Create `src/interfaces/activity-sdk.interfaces.ts`:
```typescript
export interface ActivitySdkResponse {
  success: boolean;
  status: number;
  message?: string;
}

export interface ActivitySdkClient {
  sendEvent(event: ActivityEvent): Promise<ActivitySdkResponse>;
}
```

**Deliverables**:
- Complete TypeScript interfaces for all data structures
- Type-safe definitions matching Kotlin implementation
- Interface contracts for external services

### Task 2.3: Setup HTTP Client Infrastructure
**Duration**: 1 hour  
**Description**: Configure Axios HTTP client with proper error handling and logging

**Dependencies**: 
- Phase 1 Task 1.2 (`@nestjs/axios` dependency)
- Task 2.1 (Configuration system)

Update `src/app.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { HttpModule } from '@nestjs/axios';
import configuration from '../config/configuration';

@Module({
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    HttpModule.registerAsync({
      useFactory: () => ({
        timeout: 30000,
        maxRedirects: 5,
      }),
    }),
  ],
  controllers: [],
  providers: [],
})
export class AppModule {}
```

Create `src/services/base-http.service.ts`:
```typescript
import { Injectable, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { AxiosRequestConfig, AxiosResponse } from 'axios';
import { firstValueFrom } from 'rxjs';

@Injectable()
export class BaseHttpService {
  protected readonly logger = new Logger(this.constructor.name);

  constructor(protected readonly httpService: HttpService) {}

  protected async makeRequest<T>(
    method: 'GET' | 'POST' | 'PUT' | 'DELETE',
    url: string,
    data?: any,
    config?: AxiosRequestConfig,
  ): Promise<AxiosResponse<T>> {
    try {
      this.logger.debug(`Making ${method} request to ${url}`);
      
      const requestConfig: AxiosRequestConfig = {
        method,
        url,
        data,
        ...config,
      };

      const response = await firstValueFrom(
        this.httpService.request<T>(requestConfig)
      );

      this.logger.debug(`Request successful: ${response.status}`);
      return response;
    } catch (error) {
      this.logger.error(`Request failed to ${url}:`, error.message);
      throw error;
    }
  }

  protected async get<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.makeRequest<T>('GET', url, undefined, config);
  }

  protected async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.makeRequest<T>('POST', url, data, config);
  }
}
```

**Deliverables**:
- HTTP module properly configured in AppModule
- Base HTTP service with error handling and logging
- Reusable HTTP client foundation for all services

### Task 2.4: Implement Logging Infrastructure
**Duration**: 45 minutes  
**Description**: Setup comprehensive logging system with proper formatting

**Dependencies**: 
- Task 2.3 (Base HTTP service with logging)
- Phase 1 Task 1.8 (Basic application structure)

Update `src/main.ts`:
```typescript
import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const logger = new Logger('Bootstrap');
  
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
  
  const port = process.env.PORT || 8080;
  await app.listen(port);
  logger.log(`Telegram Bot running on port ${port}`);
}

bootstrap().catch(error => {
  const logger = new Logger('Bootstrap');
  logger.error('Failed to start application:', error);
  process.exit(1);
});
```

Create `src/services/logger.service.ts`:
```typescript
import { Injectable, Logger, LogLevel } from '@nestjs/common';

@Injectable()
export class LoggerService {
  private readonly logger = new Logger(LoggerService.name);

  logMessageProcessing(
    humanReadableMessage: string,
    event: any,
    statusMessage: string
  ): void {
    const debugInfo = `
=== Message Processing Debug Info ===

Human Readable:
${humanReadableMessage}

DDC Event:
${JSON.stringify(event, null, 2)}

Activity SDK Status:
${statusMessage}
===================================`;

    this.logger.log(debugInfo);
  }

  logWebhookReceived(updateId: number, chatType?: string): void {
    this.logger.debug(`Webhook received - Update ID: ${updateId}, Chat Type: ${chatType}`);
  }

  logEventSent(status: number): void {
    this.logger.debug(`Event sent successfully (status: ${status})`);
  }

  logEventFailed(error: any): void {
    this.logger.error('Failed to send event to Activity SDK', error);
  }

  logWebhookRegistration(): void {
    this.logger.log('Registering Telegram webhook');
  }

  logWebhookRegistered(): void {
    this.logger.log('Telegram webhook registered successfully');
  }

  logUnauthorizedWebhook(): void {
    this.logger.warn('Unauthorized webhook request - token mismatch');
  }
}
```

**Deliverables**:
- Enhanced main.ts with proper logging and error handling
- Dedicated logger service with structured logging methods
- Consistent logging patterns for all operations

### Task 2.5: Create Validation Infrastructure
**Duration**: 1 hour  
**Description**: Setup validation pipeline using class-validator

**Dependencies**: 
- Phase 1 Task 1.2 (`class-validator`, `class-transformer` dependencies)
- Task 2.2 (Service interfaces)

Create `src/dto/base.dto.ts`:
```typescript
import { IsOptional, IsString, IsNumber, IsBoolean } from 'class-validator';

export class BaseDto {
  @IsOptional()
  @IsString()
  id?: string;
}

export class ValidationResponseDto {
  @IsBoolean()
  success: boolean;

  @IsOptional()
  @IsString()
  message?: string;

  @IsOptional()
  errors?: string[];
}
```

Create `src/services/validation.service.ts`:
```typescript
import { Injectable, BadRequestException } from '@nestjs/common';
import { validate, ValidationError } from 'class-validator';
import { plainToClass } from 'class-transformer';

@Injectable()
export class ValidationService {
  async validateDto<T extends object>(
    dtoClass: new () => T,
    data: any
  ): Promise<T> {
    const dto = plainToClass(dtoClass, data);
    const errors = await validate(dto);

    if (errors.length > 0) {
      const errorMessages = this.formatValidationErrors(errors);
      throw new BadRequestException({
        message: 'Validation failed',
        errors: errorMessages,
      });
    }

    return dto;
  }

  private formatValidationErrors(errors: ValidationError[]): string[] {
    return errors.flatMap(error => 
      Object.values(error.constraints || {})
    );
  }

  isValidTelegramUpdate(data: any): boolean {
    return (
      data &&
      typeof data.update_id === 'number' &&
      (data.message || data.callback_query || data.chat_member)
    );
  }

  isValidGroupMessage(message: any): boolean {
    return (
      message &&
      message.chat &&
      ['group', 'supergroup'].includes(message.chat.type) &&
      !message.text?.startsWith('/') &&
      !message.reply_to_message?.from?.is_bot
    );
  }
}
```

**Deliverables**:
- Base DTO classes with validation decorators
- Validation service with error formatting
- Telegram-specific validation helpers

### Task 2.6: Setup Error Handling Infrastructure
**Duration**: 1 hour  
**Description**: Implement comprehensive error handling and exception filters

**Dependencies**: 
- Task 2.4 (Logging infrastructure)
- Task 2.5 (Validation infrastructure)

Create `src/services/error-handler.service.ts`:
```typescript
import { Injectable, Logger, HttpException, HttpStatus } from '@nestjs/common';

@Injectable()
export class ErrorHandlerService {
  private readonly logger = new Logger(ErrorHandlerService.name);

  handleWebhookError(error: any, context: string): void {
    this.logger.error(`Webhook error in ${context}:`, error);
    
    if (error instanceof HttpException) {
      throw error;
    }
    
    throw new HttpException(
      'Internal server error during webhook processing',
      HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  handleServiceError(error: any, serviceName: string, operation: string): never {
    this.logger.error(`Service error in ${serviceName}.${operation}:`, error);
    
    if (error.response?.status === 401) {
      throw new HttpException('Unauthorized', HttpStatus.UNAUTHORIZED);
    }
    
    if (error.response?.status === 403) {
      throw new HttpException('Forbidden', HttpStatus.FORBIDDEN);
    }
    
    if (error.code === 'ECONNREFUSED' || error.code === 'ENOTFOUND') {
      throw new HttpException(
        'External service unavailable',
        HttpStatus.SERVICE_UNAVAILABLE
      );
    }
    
    throw new HttpException(
      `Service operation failed: ${operation}`,
      HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  handleCryptoError(error: any, operation: string): never {
    this.logger.error(`Crypto error in ${operation}:`, error);
    
    throw new HttpException(
      'Cryptographic operation failed',
      HttpStatus.INTERNAL_SERVER_ERROR
    );
  }

  logAndReturn<T>(result: T, operation: string): T {
    this.logger.debug(`Operation ${operation} completed successfully`);
    return result;
  }
}
```

**Deliverables**:
- Centralized error handling service
- Context-aware error logging and transformation
- HTTP exception mapping for different error types

## Validation Steps

### Validation 2.1: Configuration System Test
```bash
# Create temporary .env file
cp .env.example .env

# Test configuration loading
npm run start:dev

# Expected: Application starts without configuration errors
# Check logs for configuration loading messages
```

### Validation 2.2: Service Registration Test
```bash
# Test that all services are properly registered
npm run build

# Expected: Clean build with no dependency injection errors
# All services should compile without circular dependency issues
```

### Validation 2.3: HTTP Infrastructure Test
```bash
# Test HTTP module configuration
# Check that BaseHttpService can be instantiated
npm run start:dev

# Expected: No HTTP module configuration errors
# Application starts with proper HTTP client setup
```

### Validation 2.4: Validation Pipeline Test
```bash
# Test validation service
npm run test

# Expected: Validation methods work correctly
# DTO validation passes and fails appropriately
```

## Deliverables Summary

✅ **Configuration Management**: Type-safe configuration with environment variables  
✅ **Service Interfaces**: Complete TypeScript interfaces for all data structures  
✅ **HTTP Infrastructure**: Axios-based HTTP client with error handling  
✅ **Logging System**: Structured logging with proper formatting  
✅ **Validation Pipeline**: DTO validation with class-validator  
✅ **Error Handling**: Comprehensive error handling and exception mapping  

## Next Phase Prerequisites

Before proceeding to [Phase 3: Telegram Bot Integration](./phase-3-telegram-integration.md):

1. ✅ Configuration system loads environment variables correctly
2. ✅ All services compile and can be instantiated
3. ✅ HTTP client infrastructure is working
4. ✅ Validation pipeline processes DTOs correctly
5. ✅ Error handling service functions properly
6. ✅ Application starts without dependency injection errors

## Dependencies for Next Phase

The following components from Phase 2 will be referenced in Phase 3:
- **Configuration System**: `configuration.ts` and interfaces for Telegram config
- **HTTP Infrastructure**: `BaseHttpService` for Telegram API calls
- **Validation Service**: DTO validation for webhook payloads
- **Error Handling**: Service error handling for API failures
- **Logging Service**: Structured logging for webhook operations
- **Service Interfaces**: Telegram interfaces for API integration

Phase 3 will build upon this infrastructure by implementing the actual Telegram Bot API integration and webhook handling. 