# Phase 5: Unified SDK Integration

## Objectives
- Package and integrate the Unified SDK locally for comprehensive data ingestion
- Implement Unified SDK service for event forwarding to both DDC and Activity SDK
- Add error handling and retry logic for multi-backend operations
- Integrate with message processing pipeline using metadata-driven routing
- Implement status reporting and monitoring for all backend systems

## Prerequisites from Phase 4
- ✅ Message handler service with event creation
- ✅ Activity event DTOs and validation
- ✅ Complete message processing pipeline
- ✅ Logging infrastructure for debugging
- ✅ Crypto service with Ed25519 key generation

## Tasks

### Task 5.1: Package Unified SDK Locally (1.5 hours)
**Dependencies**: Phase 2 Task 2.1 (Configuration), Phase 4 Task 4.1 (CryptoService)

**5.1.1**: Copy Unified SDK source files to local integration:
```bash
# Create unified-sdk directory structure
mkdir -p src/unified-sdk/src
mkdir -p src/unified-sdk/types

# Copy core Unified SDK files
cp -r repos/cere-ddc-sdk-js/packages/unified/src/* src/unified-sdk/src/
cp repos/cere-ddc-sdk-js/packages/unified/package.json src/unified-sdk/
```

**5.1.2**: Create local Unified SDK package configuration:

Create `src/unified-sdk/package.json`:
```json
{
  "name": "@local/unified-sdk",
  "version": "1.0.0",
  "type": "module",
  "main": "dist/index.js",
  "types": "dist/index.d.ts",
  "dependencies": {
    "@cere-activity-sdk/ciphers": "^0.1.7",
    "@cere-activity-sdk/events": "^0.1.7", 
    "@cere-activity-sdk/signers": "^0.1.7",
    "@cere-ddc-sdk/ddc-client": "2.14.1",
    "zod": "^3.22.4"
  }
}
```

**5.1.3**: Update main project dependencies in `package.json`:
```json
"dependencies": {
  // ... existing dependencies
  "@cere-activity-sdk/ciphers": "^0.1.7",
  "@cere-activity-sdk/events": "^0.1.7",
  "@cere-activity-sdk/signers": "^0.1.7", 
  "@cere-ddc-sdk/ddc-client": "2.14.1",
  "zod": "^3.22.4"
}
```

### Task 5.2: Create Unified SDK Configuration Interface (45 minutes)
**Dependencies**: Task 5.1 (Local Unified SDK), Phase 2 Task 2.1 (Configuration)

Create `src/interfaces/unified-sdk.interfaces.ts`:
```typescript
export interface UnifiedSdkConfig {
  // DDC Client configuration
  ddcConfig: {
    signer: string; // Ed25519 private key or mnemonic
    bucketId: bigint;
    clusterId?: bigint;
    network: 'testnet' | 'devnet' | 'mainnet';
  };

  // Activity SDK configuration
  activityConfig: {
    endpoint: string;
    keyringUri: string; // Ed25519 private key for signing
    appId: string;
    connectionId?: string;
    sessionId?: string;
    appPubKey?: string;
    dataServicePubKey?: string;
  };

  // Processing configuration
  processing: {
    enableBatching: boolean;
    defaultBatchSize: number;
    defaultBatchTimeout: number; // milliseconds
    maxRetries: number;
    retryDelay: number; // milliseconds
  };

  // Logging configuration
  logging: {
    level: 'debug' | 'info' | 'warn' | 'error';
    enableMetrics: boolean;
    logRequests?: boolean;
  };
}

export interface UnifiedSdkResponse {
  transactionId: string;
  status: 'success' | 'partial' | 'failed';
  dataCloudHash?: string; // DDC CID
  indexId?: string; // Activity SDK event ID
  errors?: Array<{
    component: string;
    error: string;
    recoverable: boolean;
  }>;
  metadata: {
    processedAt: Date;
    processingTime: number; // milliseconds
    actionsExecuted: string[];
  };
}

export interface TelegramEventMetadata {
  processing: {
    dataCloudWriteMode: 'direct' | 'batch' | 'viaIndex' | 'skip';
    indexWriteMode: 'realtime' | 'skip';
    priority?: 'low' | 'normal' | 'high';
    ttl?: number; // seconds
    encryption?: boolean;
    batchOptions?: {
      maxSize?: number;
      maxWaitTime?: number; // milliseconds
    };
  };
  userContext?: Record<string, any>;
  traceId?: string;
}
```

### Task 5.3: Implement Unified SDK Service (2.5 hours)
**Dependencies**: Task 5.2 (Unified SDK Interfaces), Phase 4 Task 4.1 (CryptoService)

Create `src/services/unified-sdk.service.ts`:
```typescript
import { Injectable, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { LoggerService } from './logger.service';
import { CryptoService } from './crypto.service';
import { ErrorHandlerService } from './error-handler.service';
import { UnifiedSdkConfig, UnifiedSdkResponse, TelegramEventMetadata } from '../interfaces/unified-sdk.interfaces';
import { ActivityEventDto } from '../dto/event.dto';

// Import local Unified SDK
import { UnifiedSDK } from '../unified-sdk/src/UnifiedSDK';
import { UnifiedSDKError, ValidationError } from '../unified-sdk/src/types';

@Injectable()
export class UnifiedSdkService implements OnModuleInit {
  private unifiedSdk: UnifiedSDK;
  private sdkConfig: UnifiedSdkConfig;
  private initialized: boolean = false;

  constructor(
    private configService: ConfigService,
    private loggerService: LoggerService,
    private cryptoService: CryptoService,
    private errorHandler: ErrorHandlerService,
  ) {
    this.buildSdkConfiguration();
  }

  async onModuleInit(): Promise<void> {
    try {
      await this.initializeUnifiedSdk();
    } catch (error) {
      this.loggerService.error('Failed to initialize Unified SDK', error);
      throw error;
    }
  }

  private buildSdkConfiguration(): void {
    // Generate Ed25519 keys for DDC and Activity SDK
    const ddcSigner = this.cryptoService.generateEd25519PrivateKey();
    const activityKeyring = this.cryptoService.generateEd25519PrivateKey();

    this.sdkConfig = {
      ddcConfig: {
        signer: ddcSigner,
        bucketId: BigInt(this.configService.get<string>('ddc.bucketId', '12345')),
        clusterId: this.configService.get<string>('ddc.clusterId') 
          ? BigInt(this.configService.get<string>('ddc.clusterId'))
          : undefined,
        network: this.configService.get<'testnet' | 'devnet' | 'mainnet'>('ddc.network', 'testnet'),
      },
      activityConfig: {
        endpoint: this.configService.get<string>('event.serviceUrl', 'https://api.stats.cere.network'),
        keyringUri: activityKeyring,
        appId: this.configService.get<string>('event.appId', 'telegram-bot'),
        connectionId: this.configService.get<string>('event.connectionId'),
        sessionId: this.configService.get<string>('event.sessionId'),
        appPubKey: this.configService.get<string>('event.userPubKey'),
        dataServicePubKey: this.configService.get<string>('event.dataServiceId'),
      },
      processing: {
        enableBatching: this.configService.get<boolean>('unified.enableBatching', true),
        defaultBatchSize: this.configService.get<number>('unified.defaultBatchSize', 100),
        defaultBatchTimeout: this.configService.get<number>('unified.defaultBatchTimeout', 5000),
        maxRetries: this.configService.get<number>('unified.maxRetries', 3),
        retryDelay: this.configService.get<number>('unified.retryDelay', 1000),
      },
      logging: {
        level: this.configService.get<'debug' | 'info' | 'warn' | 'error'>('unified.logLevel', 'info'),
        enableMetrics: this.configService.get<boolean>('unified.enableMetrics', true),
        logRequests: this.configService.get<boolean>('unified.logRequests', false),
      },
    };
  }

  private async initializeUnifiedSdk(): Promise<void> {
    this.loggerService.info('Initializing Unified SDK with configuration', {
      ddcNetwork: this.sdkConfig.ddcConfig.network,
      activityEndpoint: this.sdkConfig.activityConfig.endpoint,
      batchingEnabled: this.sdkConfig.processing.enableBatching,
    });

    this.unifiedSdk = new UnifiedSDK(this.sdkConfig);
    await this.unifiedSdk.initialize();
    this.initialized = true;

    this.loggerService.info('Unified SDK initialized successfully');
  }

  async sendTelegramEvent(
    event: ActivityEventDto,
    options?: {
      priority?: 'low' | 'normal' | 'high';
      encryption?: boolean;
      writeMode?: 'realtime' | 'batch';
    }
  ): Promise<UnifiedSdkResponse> {
    if (!this.initialized) {
      throw new Error('Unified SDK not initialized');
    }

    try {
      // Create metadata for Telegram event processing
      const metadata: TelegramEventMetadata = {
        processing: {
          dataCloudWriteMode: options?.writeMode === 'batch' ? 'batch' : 'direct',
          indexWriteMode: 'realtime', // Always index Telegram events
          priority: options?.priority || 'normal',
          encryption: options?.encryption || false,
          ttl: 86400 * 7, // 7 days retention
          ...(options?.writeMode === 'batch' && {
            batchOptions: {
              maxSize: this.sdkConfig.processing.defaultBatchSize,
              maxWaitTime: this.sdkConfig.processing.defaultBatchTimeout,
            },
          }),
        },
        userContext: {
          source: 'telegram-bot',
          eventType: event.event_type,
          accountId: event.account_id,
        },
        traceId: `telegram-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      };

      // Transform ActivityEventDto to Telegram event format for Unified SDK
      const telegramEventPayload = {
        eventType: event.event_type,
        userId: event.account_id,
        chatId: event.payload.chatId,
        eventData: event.payload,
        timestamp: new Date(event.timestamp),
        messageId: event.payload.messageId,
        messageType: event.payload.messageType,
      };

      this.loggerService.debug('Sending Telegram event to Unified SDK', {
        traceId: metadata.traceId,
        eventType: event.event_type,
        accountId: event.account_id,
      });

      const result = await this.unifiedSdk.writeData(telegramEventPayload, {
        priority: options?.priority,
        encryption: options?.encryption,
        writeMode: options?.writeMode,
        metadata,
      });

      this.loggerService.info('Telegram event processed successfully', {
        transactionId: result.transactionId,
        status: result.status,
        processingTime: result.metadata.processingTime,
        dataCloudHash: result.dataCloudHash,
        indexId: result.indexId,
      });

      return result;
    } catch (error) {
      this.loggerService.error('Failed to send Telegram event', error);
      
      if (error instanceof ValidationError) {
        throw new Error(`Validation failed: ${error.message}`);
      } else if (error instanceof UnifiedSDKError) {
        if (error.recoverable) {
          throw new Error(`Recoverable error: ${error.message}`);
        } else {
          throw new Error(`Non-recoverable error: ${error.message}`);
        }
      }
      
      throw error;
    }
  }

  async sendTelegramEventWithRetry(
    event: ActivityEventDto,
    options?: {
      priority?: 'low' | 'normal' | 'high';
      encryption?: boolean;
      writeMode?: 'realtime' | 'batch';
      maxRetries?: number;
    }
  ): Promise<UnifiedSdkResponse> {
    const maxRetries = options?.maxRetries || this.sdkConfig.processing.maxRetries;
    let lastError: any;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return await this.sendTelegramEvent(event, options);
      } catch (error) {
        lastError = error;

        if (this.shouldRetry(error) && attempt < maxRetries) {
          const delay = Math.pow(2, attempt) * this.sdkConfig.processing.retryDelay;
          this.loggerService.warn(`Attempt ${attempt} failed, retrying in ${delay}ms`, error);
          await this.sleep(delay);
          continue;
        }

        this.loggerService.error(`All ${maxRetries} attempts failed`, error);
        throw error;
      }
    }

    throw lastError;
  }

  async getSystemHealth(): Promise<{
    status: 'healthy' | 'degraded' | 'unhealthy';
    components: {
      unifiedSdk: boolean;
      ddcClient: boolean;
      activitySdk: boolean;
    };
    timestamp: string;
  }> {
    try {
      const sdkStatus = this.unifiedSdk?.getStatus();
      
      return {
        status: this.initialized && sdkStatus?.initialized ? 'healthy' : 'unhealthy',
        components: {
          unifiedSdk: this.initialized,
          ddcClient: sdkStatus?.components?.orchestrator || false,
          activitySdk: sdkStatus?.components?.orchestrator || false,
        },
        timestamp: new Date().toISOString(),
      };
    } catch (error) {
      this.loggerService.error('Health check failed', error);
      return {
        status: 'unhealthy',
        components: {
          unifiedSdk: false,
          ddcClient: false,
          activitySdk: false,
        },
        timestamp: new Date().toISOString(),
      };
    }
  }

  private shouldRetry(error: any): boolean {
    // Retry on network errors, timeouts, or recoverable Unified SDK errors
    return (
      error.code === 'ECONNREFUSED' ||
      error.code === 'ENOTFOUND' ||
      error.code === 'ETIMEDOUT' ||
      (error instanceof UnifiedSDKError && error.recoverable) ||
      (error.response?.status >= 500 && error.response?.status < 600)
    );
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  async cleanup(): Promise<void> {
    if (this.unifiedSdk && this.initialized) {
      await this.unifiedSdk.cleanup();
      this.initialized = false;
      this.loggerService.info('Unified SDK cleanup completed');
    }
  }
}
```

### Task 5.4: Update Message Handler with Unified SDK (1.5 hours)
**Dependencies**: Task 5.3 (UnifiedSdkService), Phase 4 Task 4.3 (MessageHandlerService)

Update `src/services/message-handler.service.ts`:
```typescript
// Add to imports
import { UnifiedSdkService } from './unified-sdk.service';
import { UnifiedSdkResponse } from '../interfaces/unified-sdk.interfaces';

// Update constructor
constructor(
  private configService: ConfigService,
  private cryptoService: CryptoService,  
  private loggerService: LoggerService,
  private unifiedSdkService: UnifiedSdkService, // Replace ActivitySdkService
) {}

// Update handle method
async handle(update: TelegramUpdateDto): Promise<void> {
  try {
    const message = update.message;
    if (!message) return;

    const chat = message.chat;
    const humanReadableMessage = this.createHumanReadableMessage(message, chat);
    const payload = this.createMessagePayload(message, chat);
    
    const accountId = message.from 
      ? await this.cryptoService.generateAccountId(message.from.id)
      : this.configService.get<string>('event.accountId');

    const event: ActivityEventDto = {
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

    // Send event via Unified SDK with intelligent routing
    let statusMessage: string;
    let unifiedResponse: UnifiedSdkResponse | null = null;
    
    try {
      // Determine processing options based on message type and content
      const processingOptions = this.determineProcessingOptions(message, payload);
      
      unifiedResponse = await this.unifiedSdkService.sendTelegramEventWithRetry(
        event, 
        processingOptions
      );
      
      statusMessage = this.createSuccessMessage(unifiedResponse);
    } catch (error) {
      statusMessage = `❌ Failed to send event via Unified SDK: ${error.message}`;
    }

    // Log complete debug information with Unified SDK details
    this.loggerService.logMessageProcessing(
      humanReadableMessage, 
      event, 
      statusMessage,
      unifiedResponse
    );
  } catch (error) {
    this.loggerService.error('Error processing group message', error);
  }
}

private determineProcessingOptions(message: any, payload: any): {
  priority: 'low' | 'normal' | 'high';
  encryption: boolean;
  writeMode: 'realtime' | 'batch';
} {
  // High priority for media messages or messages with special content
  const isHighPriority = message.photo || message.video || 
                        (message.text && message.text.includes('!important'));
  
  // Enable encryption for sensitive content
  const needsEncryption = message.text && 
                         (message.text.includes('private') || message.text.includes('secret'));
  
  // Use batch mode for regular text messages, realtime for media
  const writeMode = (message.photo || message.video) ? 'realtime' : 'batch';
  
  return {
    priority: isHighPriority ? 'high' : 'normal',
    encryption: needsEncryption,
    writeMode,
  };
}

private createSuccessMessage(response: UnifiedSdkResponse): string {
  const parts = [`✅ Event processed via Unified SDK (${response.status})`];
  
  if (response.dataCloudHash) {
    parts.push(`DDC: ${response.dataCloudHash.substring(0, 8)}...`);
  }
  
  if (response.indexId) {
    parts.push(`Index: ${response.indexId}`);
  }
  
  parts.push(`Time: ${response.metadata.processingTime}ms`);
  parts.push(`Actions: [${response.metadata.actionsExecuted.join(', ')}]`);
  
  return parts.join(' | ');
}
```

### Task 5.5: Create Health Check Service for Unified SDK (1 hour)
**Dependencies**: Task 5.3 (UnifiedSdkService)

Create `src/services/health.service.ts`:
```typescript
import { Injectable } from '@nestjs/common';
import { UnifiedSdkService } from './unified-sdk.service';

@Injectable()
export class HealthService {
  constructor(private unifiedSdkService: UnifiedSdkService) {}

  async getSystemHealth(): Promise<{
    status: 'healthy' | 'degraded' | 'unhealthy';
    services: {
      unifiedSdk: {
        healthy: boolean;
        components: {
          ddcClient: boolean;
          activitySdk: boolean;
        };
      };
      telegram: {
        healthy: boolean;
        webhook: boolean;
      };
    };
    timestamp: string;
  }> {
    const unifiedSdkHealth = await this.unifiedSdkService.getSystemHealth();
    
    // Telegram service is healthy if the application is running
    const telegramHealth = {
      healthy: true,
      webhook: true, // Assume webhook is working if we're processing requests
    };
    
    // Overall status determination
    let overallStatus: 'healthy' | 'degraded' | 'unhealthy';
    if (unifiedSdkHealth.status === 'healthy' && telegramHealth.healthy) {
      overallStatus = 'healthy';
    } else if (unifiedSdkHealth.status === 'degraded' || !telegramHealth.healthy) {
      overallStatus = 'degraded';
    } else {
      overallStatus = 'unhealthy';
    }
    
    return {
      status: overallStatus,
      services: {
        unifiedSdk: {
          healthy: unifiedSdkHealth.status === 'healthy',
          components: {
            ddcClient: unifiedSdkHealth.components.ddcClient,
            activitySdk: unifiedSdkHealth.components.activitySdk,
          },
        },
        telegram: telegramHealth,
      },
      timestamp: new Date().toISOString(),
    };
  }

  async checkUnifiedSdk(): Promise<{
    healthy: boolean;
    status: string;
    components: Record<string, boolean>;
  }> {
    const health = await this.unifiedSdkService.getSystemHealth();
    
    return {
      healthy: health.status === 'healthy',
      status: health.status,
      components: health.components,
    };
  }
}
```

### Task 5.6: Add Health Check Controller (45 minutes)
**Dependencies**: Task 5.5 (HealthService)

Create `src/controllers/health.controller.ts`:
```typescript
import { Controller, Get } from '@nestjs/common';
import { HealthService } from '../services/health.service';

@Controller('health')
export class HealthController {
  constructor(private healthService: HealthService) {}

  @Get()
  async getHealth() {
    return this.healthService.getSystemHealth();
  }

  @Get('unified-sdk')
  async getUnifiedSdkHealth() {
    return this.healthService.checkUnifiedSdk();
  }

  @Get('components')
  async getComponentsHealth() {
    const health = await this.healthService.getSystemHealth();
    return {
      components: health.services,
      timestamp: health.timestamp,
    };
  }
}
```

### Task 5.7: Update Configuration for Unified SDK (30 minutes)
**Dependencies**: Task 5.2 (Unified SDK Interfaces)

Update `src/config/configuration.ts`:
```typescript
// Add to existing configuration
export default () => ({
  // ... existing config
  
  // DDC Configuration for Unified SDK
  ddc: {
    bucketId: process.env.DDC_BUCKET_ID || '12345',
    clusterId: process.env.DDC_CLUSTER_ID,
    network: process.env.DDC_NETWORK || 'testnet',
  },
  
  // Unified SDK Processing Configuration
  unified: {
    enableBatching: process.env.UNIFIED_ENABLE_BATCHING === 'true',
    defaultBatchSize: parseInt(process.env.UNIFIED_DEFAULT_BATCH_SIZE || '100'),
    defaultBatchTimeout: parseInt(process.env.UNIFIED_DEFAULT_BATCH_TIMEOUT || '5000'),
    maxRetries: parseInt(process.env.UNIFIED_MAX_RETRIES || '3'),
    retryDelay: parseInt(process.env.UNIFIED_RETRY_DELAY || '1000'),
    logLevel: process.env.UNIFIED_LOG_LEVEL || 'info',
    enableMetrics: process.env.UNIFIED_ENABLE_METRICS === 'true',
    logRequests: process.env.UNIFIED_LOG_REQUESTS === 'true',
  },
});
```

Update `.env.example`:
```bash
# ... existing environment variables

# DDC Configuration
DDC_BUCKET_ID=12345
DDC_CLUSTER_ID=
DDC_NETWORK=testnet

# Unified SDK Configuration
UNIFIED_ENABLE_BATCHING=true
UNIFIED_DEFAULT_BATCH_SIZE=100
UNIFIED_DEFAULT_BATCH_TIMEOUT=5000
UNIFIED_MAX_RETRIES=3
UNIFIED_RETRY_DELAY=1000
UNIFIED_LOG_LEVEL=info
UNIFIED_ENABLE_METRICS=true
UNIFIED_LOG_REQUESTS=false
```

### Task 5.8: Update App Module with Unified SDK Integration (30 minutes)
**Dependencies**: All Phase 5 services and interfaces

Update `src/app.module.ts`:
```typescript
// Add imports
import { UnifiedSdkService } from './services/unified-sdk.service';
import { HealthService } from './services/health.service';
import { HealthController } from './controllers/health.controller';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [configuration],
    }),
    HttpModule,
  ],
  controllers: [
    WebhookController,
    HealthController, // Add health controller
  ],
  providers: [
    // Core services from previous phases
    BaseHttpService,
    LoggerService,
    ValidationService,
    ErrorHandlerService,
    TelegramBotService,
    WebhookService,
    CryptoService,
    MessageHandlerService,
    
    // Phase 5: Unified SDK services
    UnifiedSdkService,
    HealthService,
  ],
  exports: [
    // Export services for potential use in other modules
    UnifiedSdkService,
    HealthService,
    LoggerService,
    CryptoService,
  ],
})
export class AppModule implements OnModuleDestroy {
  constructor(private unifiedSdkService: UnifiedSdkService) {}

  async onModuleDestroy() {
    // Cleanup Unified SDK resources on shutdown
    await this.unifiedSdkService.cleanup();
  }
}
```

### Task 5.9: Update Logger Service for Unified SDK (30 minutes)
**Dependencies**: Task 5.3 (UnifiedSdkService), Phase 2 Task 2.4 (LoggerService)

Update `src/services/logger.service.ts`:
```typescript
// Add method for Unified SDK logging
logMessageProcessing(
  humanReadableMessage: string,
  event: any,
  statusMessage: string,
  unifiedResponse?: any
): void {
  const logData = {
    humanReadableMessage,
    event: {
      account_id: event.account_id,
      event_type: event.event_type,
      timestamp: event.timestamp,
      payload_keys: Object.keys(event.payload || {}),
    },
    statusMessage,
    ...(unifiedResponse && {
      unifiedSdkResponse: {
        transactionId: unifiedResponse.transactionId,
        status: unifiedResponse.status,
        dataCloudHash: unifiedResponse.dataCloudHash,
        indexId: unifiedResponse.indexId,
        processingTime: unifiedResponse.metadata?.processingTime,
        actionsExecuted: unifiedResponse.metadata?.actionsExecuted,
      },
    }),
  };

  this.log('info', 'Message processed via Unified SDK', logData);
}

logUnifiedSdkHealth(healthStatus: any): void {
  this.log('info', 'Unified SDK health check', {
    status: healthStatus.status,
    components: healthStatus.components,
    timestamp: healthStatus.timestamp,
  });
}
```

## Validation Steps

### Task 5.10: Build and Test Integration (45 minutes)
**Dependencies**: All Phase 5 tasks completed

**5.10.1**: Install new dependencies:
```bash
npm install @cere-activity-sdk/ciphers @cere-activity-sdk/events @cere-activity-sdk/signers @cere-ddc-sdk/ddc-client zod
```

**5.10.2**: Build the application:
```bash
npm run build
```

**5.10.3**: Test health endpoints:
```bash
# Start the application
npm run start:dev

# Test health endpoints
curl http://localhost:3000/health
curl http://localhost:3000/health/unified-sdk
curl http://localhost:3000/health/components
```

**5.10.4**: Test Telegram webhook with Unified SDK integration:
```bash
# Send test webhook payload
curl -X POST http://localhost:3000/webhook \
  -H "Content-Type: application/json" \
  -H "X-Telegram-Bot-Api-Secret-Token: your-secret-token" \
  -d '{
    "update_id": 123456789,
    "message": {
      "message_id": 1,
      "from": {
        "id": 12345,
        "is_bot": false,
        "first_name": "Test",
        "username": "testuser"
      },
      "chat": {
        "id": -67890,
        "title": "Test Group",
        "type": "supergroup"
      },
      "date": 1640995200,
      "text": "Hello from Unified SDK test!"
    }
  }'
```

## Success Criteria
- ✅ Unified SDK successfully packaged and integrated locally
- ✅ Configuration properly set up for DDC and Activity SDK backends
- ✅ Message handler successfully routes events through Unified SDK
- ✅ Health checks report status of all Unified SDK components
- ✅ Events are processed with metadata-driven routing (DDC + Activity SDK)
- ✅ Error handling and retry logic working for multi-backend operations
- ✅ Logging captures complete Unified SDK processing information
- ✅ Application builds and runs without errors
- ✅ Webhook processing works end-to-end with Unified SDK integration

## Next Steps
- **Phase 6**: Production deployment and monitoring setup
- **Phase 7**: Performance optimization and advanced features

## Files Modified/Created in Phase 5
- `src/unified-sdk/` - Local Unified SDK package
- `src/interfaces/unified-sdk.interfaces.ts` - Unified SDK interfaces
- `src/services/unified-sdk.service.ts` - Main Unified SDK service
- `src/services/health.service.ts` - Health monitoring service
- `src/controllers/health.controller.ts` - Health check endpoints
- `src/services/message-handler.service.ts` - Updated with Unified SDK
- `src/services/logger.service.ts` - Enhanced logging
- `src/config/configuration.ts` - Unified SDK configuration
- `src/app.module.ts` - Module registration and cleanup
- `package.json` - New dependencies
- `.env.example` - Unified SDK environment variables 