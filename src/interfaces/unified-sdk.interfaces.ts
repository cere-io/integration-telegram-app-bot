import { UnifiedSDKConfig as BaseUnifiedSDKConfig, UnifiedMetadata, UnifiedResponse } from '../unified-sdk/src/types';

/**
 * Extended Unified SDK Configuration for Telegram Bot Integration
 * Extends the base UnifiedSDK config with bot-specific settings
 */
export interface TelegramUnifiedSDKConfig extends BaseUnifiedSDKConfig {
  // Telegram-specific configuration
  telegramConfig: {
    botToken: string;
    webhookSecret: string;
    allowedUpdates: string[];
    messageProcessing: {
      enableBatching: boolean;
      batchSize: number;
      batchTimeout: number; // in milliseconds
      priorityMapping: {
        userMessages: 'low' | 'normal' | 'high';
        botCommands: 'low' | 'normal' | 'high';
        mediaMessages: 'low' | 'normal' | 'high';
        systemEvents: 'low' | 'normal' | 'high';
      };
    };
    errorHandling: {
      maxRetries: number;
      retryDelay: number; // in milliseconds
      fallbackToActivity: boolean;
      fallbackToDDC: boolean;
    };
  };

  // Override processing settings for Telegram use case
  processing: BaseUnifiedSDKConfig['processing'] & {
    telegramSpecific: {
      autoDetectMessageType: boolean;
      preserveMediaLinks: boolean;
      enableEventDeduplication: boolean;
      deduplicationWindow: number; // in milliseconds
    };
  };
}

/**
 * Unified SDK Service Status Interface
 */
export interface UnifiedSDKServiceStatus {
  initialized: boolean;
  connectionStatus: {
    
    activitySDK: 'connected' | 'disconnected' | 'error' | 'not_configured';
  };
  lastHealthCheck: Date;
  errorCount: number;
  successCount: number;
  averageResponseTime: number; // in milliseconds
  components: {
    rulesInterpreter: boolean;
    dispatcher: boolean;
    orchestrator: boolean;
  };
}

/**
 * Extended Unified Response with Telegram-specific metadata
 */
export interface TelegramUnifiedResponse extends UnifiedResponse {
  telegramMetadata?: {
    messageId?: string;
    chatId?: string;
    userId?: string;
    messageType?: string;
    processingMode: 'realtime' | 'batch';
    deduplicationApplied: boolean;
  };
}

/**
 * Telegram Event Processing Options
 */
export interface TelegramEventOptions {
  priority?: 'low' | 'normal' | 'high';
  encryption?: boolean;
  writeMode?: 'realtime' | 'batch';
  metadata?: Partial<UnifiedMetadata>;
  telegramSpecific?: {
    preserveOriginalMessage: boolean;
    enableAnalytics: boolean;
    customTags?: string[];
    chatContext?: {
      chatType: 'private' | 'group' | 'supergroup' | 'channel';
      memberCount?: number;
      isBot?: boolean;
    };
  };
}

/**
 * Batch Processing Configuration
 */
export interface BatchProcessingConfig {
  enabled: boolean;
  maxBatchSize: number;
  maxWaitTime: number; // in milliseconds
  flushTriggers: {
    onSize: boolean;
    onTime: boolean;
    onPriority: boolean; // Flush immediately for high priority
    onShutdown: boolean;
  };
  retryPolicy: {
    maxRetries: number;
    retryDelay: number; // in milliseconds
    exponentialBackoff: boolean;
  };
}

/**
 * Error Recovery Configuration
 */
export interface ErrorRecoveryConfig {
  enableAutoRecovery: boolean;
  maxRecoveryAttempts: number;
  recoveryStrategies: {
    ddcFailure: 'retry' | 'fallback_activity' | 'skip';
    activityFailure: 'retry' | 'fallback_ddc' | 'skip';
    networkFailure: 'retry' | 'queue' | 'skip';
    validationFailure: 'retry' | 'log_and_skip' | 'throw';
  };
  circuitBreaker: {
    enabled: boolean;
    failureThreshold: number;
    recoveryTimeout: number; // in milliseconds
    halfOpenMaxCalls: number;
  };
}

/**
 * Monitoring and Metrics Configuration
 */
export interface MonitoringConfig {
  enableMetrics: boolean;
  metricsInterval: number; // in milliseconds
  healthCheckInterval: number; // in milliseconds
  alerting: {
    enabled: boolean;
    thresholds: {
      errorRate: number; // percentage
      responseTime: number; // in milliseconds
      queueSize: number;
    };
    webhookUrl?: string;
  };
  logging: {
    level: 'debug' | 'info' | 'warn' | 'error';
    enableStructuredLogging: boolean;
    includeStackTrace: boolean;
    logToFile: boolean;
    maxLogSize: number; // in bytes
  };
}

/**
 * Complete Unified SDK Service Configuration
 */
export interface UnifiedSDKServiceConfig {
  unifiedSDK: TelegramUnifiedSDKConfig;
  batchProcessing: BatchProcessingConfig;
  errorRecovery: ErrorRecoveryConfig;
  monitoring: MonitoringConfig;
  development: {
    enableTestMode: boolean;
    mockBackends: boolean;
    debugMode: boolean;
    verboseLogging: boolean;
  };
}

/**
 * Health Check Interfaces
 */
export interface HealthCheckResult {
  status: 'healthy' | 'degraded' | 'unhealthy';
  message: string;
  error?: string;
  lastChecked: Date;
  details?: Record<string, any>;
}

export interface ComponentHealth extends HealthCheckResult {}

export interface SystemHealthStatus {
  status: 'healthy' | 'degraded' | 'unhealthy';
  timestamp: Date;
  uptime: number;
  version: string;
  environment: string;
  components: Record<string, ComponentHealth>;
  responseTime: number;
  memory: {
    used: number;
    total: number;
    external: number;
  };
} 