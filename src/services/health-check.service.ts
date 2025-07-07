import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { UnifiedSdkService } from './unified-sdk.service';
import { TelegramBotService } from './telegram-bot.service';
import { 
  SystemHealthStatus, 
  ComponentHealth, 
  HealthCheckResult 
} from '../interfaces/unified-sdk.interfaces';

@Injectable()
export class HealthCheckService {
  private readonly logger = new Logger(HealthCheckService.name);

  constructor(
    private configService: ConfigService,
    private unifiedSdkService: UnifiedSdkService,
    private telegramBotService: TelegramBotService,
  ) {}

  async getSystemHealth(): Promise<SystemHealthStatus> {
    const startTime = Date.now();
    
    try {
      // Check all system components
      const [
        unifiedSdkHealth,
        telegramBotHealth,
        configHealth,
      ] = await Promise.allSettled([
        this.checkUnifiedSdk(),
        this.checkTelegramBot(),
        this.checkConfiguration(),
      ]);

      const components: Record<string, ComponentHealth> = {
        unifiedSdk: this.resolveHealthResult(unifiedSdkHealth),
        telegramBot: this.resolveHealthResult(telegramBotHealth),
        configuration: this.resolveHealthResult(configHealth),
      };

      // Determine overall system status
      const componentStatuses = Object.values(components).map(c => c.status);
      const overallStatus = this.determineOverallStatus(componentStatuses);

      const healthStatus: SystemHealthStatus = {
        status: overallStatus,
        timestamp: new Date(),
        uptime: process.uptime(),
        version: this.configService.get<string>('app.version', '1.0.0'),
        environment: this.configService.get<string>('NODE_ENV', 'development'),
        components,
        responseTime: Date.now() - startTime,
        memory: {
          used: process.memoryUsage().heapUsed,
          total: process.memoryUsage().heapTotal,
          external: process.memoryUsage().external,
        },
      };

      this.logger.debug('System health check completed', {
        status: overallStatus,
        responseTime: healthStatus.responseTime,
        componentCount: Object.keys(components).length,
      });

      return healthStatus;
    } catch (error) {
      this.logger.error('Health check failed', error);
      
      return {
        status: 'unhealthy',
        timestamp: new Date(),
        uptime: process.uptime(),
        version: this.configService.get<string>('app.version', '1.0.0'),
        environment: this.configService.get<string>('NODE_ENV', 'development'),
        components: {
          system: {
            status: 'unhealthy',
            message: 'Health check system failure',
            error: error.message,
            lastChecked: new Date(),
          }
        },
        responseTime: Date.now() - startTime,
        memory: {
          used: process.memoryUsage().heapUsed,
          total: process.memoryUsage().heapTotal,
          external: process.memoryUsage().external,
        },
      };
    }
  }

  private async checkUnifiedSdk(): Promise<HealthCheckResult> {
    try {
      if (!this.unifiedSdkService.isInitialized()) {
        return {
          status: 'unhealthy',
          message: 'Unified SDK not initialized',
          lastChecked: new Date(),
        };
      }

      const sdkHealth = await this.unifiedSdkService.getSystemHealth();
      
      return {
        status: sdkHealth.initialized ? 'healthy' : 'unhealthy',
        message: sdkHealth.initialized 
          ? `SDK operational (${sdkHealth.successCount} successful operations)` 
          : 'SDK not properly initialized',
        lastChecked: new Date(),
        details: {
          initialized: sdkHealth.initialized,
          errorCount: sdkHealth.errorCount,
          successCount: sdkHealth.successCount,
          averageResponseTime: sdkHealth.averageResponseTime,
          connections: sdkHealth.connectionStatus,
          components: sdkHealth.components,
        },
      };
    } catch (error) {
      return {
        status: 'unhealthy',
        message: 'Unified SDK health check failed',
        error: error.message,
        lastChecked: new Date(),
      };
    }
  }

  private async checkTelegramBot(): Promise<HealthCheckResult> {
    try {
      // Basic check to see if Telegram bot service is available
      const botToken = this.configService.get<string>('telegram.botToken');
      
      if (!botToken) {
        return {
          status: 'unhealthy',
          message: 'Telegram bot token not configured',
          lastChecked: new Date(),
        };
      }

      return {
        status: 'healthy',
        message: 'Telegram bot service operational',
        lastChecked: new Date(),
        details: {
          tokenConfigured: !!botToken,
          webhookConfigured: !!this.configService.get<string>('telegram.webhookSecret'),
        },
      };
    } catch (error) {
      return {
        status: 'unhealthy',
        message: 'Telegram bot check failed',
        error: error.message,
        lastChecked: new Date(),
      };
    }
  }

  private async checkConfiguration(): Promise<HealthCheckResult> {
    try {
      const requiredConfigs = [
        'telegram.botToken',
        'event.appId',
        
      ];

      const missingConfigs = requiredConfigs.filter(
        config => !this.configService.get(config)
      );

      if (missingConfigs.length > 0) {
        return {
          status: 'degraded',
          message: `Missing configurations: ${missingConfigs.join(', ')}`,
          lastChecked: new Date(),
          details: {
            missingConfigs,
            totalRequired: requiredConfigs.length,
          },
        };
      }

      return {
        status: 'healthy',
        message: 'All required configurations present',
        lastChecked: new Date(),
        details: {
          configuredItems: requiredConfigs.length,
        },
      };
    } catch (error) {
      return {
        status: 'unhealthy',
        message: 'Configuration check failed',
        error: error.message,
        lastChecked: new Date(),
      };
    }
  }

  private resolveHealthResult(result: PromiseSettledResult<HealthCheckResult>): ComponentHealth {
    if (result.status === 'fulfilled') {
      return result.value;
    } else {
      return {
        status: 'unhealthy',
        message: 'Component check failed',
        error: result.reason?.message || 'Unknown error',
        lastChecked: new Date(),
      };
    }
  }

  private determineOverallStatus(componentStatuses: Array<'healthy' | 'degraded' | 'unhealthy'>): 'healthy' | 'degraded' | 'unhealthy' {
    if (componentStatuses.every(status => status === 'healthy')) {
      return 'healthy';
    }
    
    if (componentStatuses.some(status => status === 'unhealthy')) {
      return 'unhealthy';
    }
    
    return 'degraded';
  }

  async getReadinessCheck(): Promise<{ ready: boolean; message: string }> {
    try {
      const health = await this.getSystemHealth();
      const ready = health.status === 'healthy' || health.status === 'degraded';
      
      return {
        ready,
        message: ready ? 'Service is ready' : 'Service is not ready',
      };
    } catch (error) {
      return {
        ready: false,
        message: `Readiness check failed: ${error.message}`,
      };
    }
  }

  async getLivenessCheck(): Promise<{ alive: boolean; message: string }> {
    try {
      // Basic liveness check - if we can respond, we're alive
      return {
        alive: true,
        message: 'Service is alive',
      };
    } catch (error) {
      return {
        alive: false,
        message: `Liveness check failed: ${error.message}`,
      };
    }
  }
} 