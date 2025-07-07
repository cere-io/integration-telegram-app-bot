import { Controller, Get, HttpStatus, Res } from '@nestjs/common';
import { Response } from 'express';
import { HealthCheckService } from '../services/health-check.service';
import { SystemHealthStatus } from '../interfaces/unified-sdk.interfaces';

@Controller('health')
export class HealthCheckController {
  constructor(private readonly healthCheckService: HealthCheckService) {}

  /**
   * Comprehensive health check endpoint
   * Returns detailed system health information
   */
  @Get()
  async getHealth(@Res() res: Response): Promise<void> {
    try {
      const health: SystemHealthStatus = await this.healthCheckService.getSystemHealth();
      
      const statusCode = this.getStatusCode(health.status);
      res.status(statusCode).json(health);
    } catch (error) {
      res.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        status: 'unhealthy',
        timestamp: new Date(),
        error: error.message,
        message: 'Health check failed',
      });
    }
  }

  /**
   * Kubernetes/Docker readiness probe endpoint
   * Returns 200 if service is ready to accept traffic
   */
  @Get('ready')
  async getReadiness(@Res() res: Response): Promise<void> {
    try {
      const readiness = await this.healthCheckService.getReadinessCheck();
      
      const statusCode = readiness.ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
      res.status(statusCode).json({
        ready: readiness.ready,
        message: readiness.message,
        timestamp: new Date(),
      });
    } catch (error) {
      res.status(HttpStatus.SERVICE_UNAVAILABLE).json({
        ready: false,
        message: `Readiness check failed: ${error.message}`,
        timestamp: new Date(),
      });
    }
  }

  /**
   * Kubernetes/Docker liveness probe endpoint
   * Returns 200 if service is alive
   */
  @Get('live')
  async getLiveness(@Res() res: Response): Promise<void> {
    try {
      const liveness = await this.healthCheckService.getLivenessCheck();
      
      const statusCode = liveness.alive ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
      res.status(statusCode).json({
        alive: liveness.alive,
        message: liveness.message,
        timestamp: new Date(),
      });
    } catch (error) {
      res.status(HttpStatus.SERVICE_UNAVAILABLE).json({
        alive: false,
        message: `Liveness check failed: ${error.message}`,
        timestamp: new Date(),
      });
    }
  }

  /**
   * Unified SDK specific health endpoint
   * Returns detailed information about the Unified SDK status
   */
  @Get('unified-sdk')
  async getUnifiedSdkHealth(@Res() res: Response): Promise<void> {
    try {
      const health = await this.healthCheckService.getSystemHealth();
      const unifiedSdkHealth = health.components.unifiedSdk;
      
      if (!unifiedSdkHealth) {
        res.status(HttpStatus.SERVICE_UNAVAILABLE).json({
          status: 'unhealthy',
          message: 'Unified SDK component not found',
          timestamp: new Date(),
        });
        return;
      }

      const statusCode = this.getStatusCode(unifiedSdkHealth.status);
      res.status(statusCode).json({
        status: unifiedSdkHealth.status,
        message: unifiedSdkHealth.message,
        details: unifiedSdkHealth.details,
        lastChecked: unifiedSdkHealth.lastChecked,
        timestamp: new Date(),
      });
    } catch (error) {
      res.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        status: 'unhealthy',
        message: `Unified SDK health check failed: ${error.message}`,
        timestamp: new Date(),
      });
    }
  }

  /**
   * Simple ping endpoint for basic connectivity tests
   */
  @Get('ping')
  async ping(@Res() res: Response): Promise<void> {
    res.status(HttpStatus.OK).json({
      message: 'pong',
      timestamp: new Date(),
      uptime: process.uptime(),
    });
  }

  /**
   * System metrics endpoint
   */
  @Get('metrics')
  async getMetrics(@Res() res: Response): Promise<void> {
    try {
      const health = await this.healthCheckService.getSystemHealth();
      
      const metrics = {
        timestamp: new Date(),
        uptime: health.uptime,
        memory: health.memory,
        responseTime: health.responseTime,
        environment: health.environment,
        version: health.version,
        componentCount: Object.keys(health.components).length,
        healthyComponents: Object.values(health.components).filter(c => c.status === 'healthy').length,
        degradedComponents: Object.values(health.components).filter(c => c.status === 'degraded').length,
        unhealthyComponents: Object.values(health.components).filter(c => c.status === 'unhealthy').length,
      };

      res.status(HttpStatus.OK).json(metrics);
    } catch (error) {
      res.status(HttpStatus.INTERNAL_SERVER_ERROR).json({
        error: 'Failed to retrieve metrics',
        message: error.message,
        timestamp: new Date(),
      });
    }
  }

  private getStatusCode(status: 'healthy' | 'degraded' | 'unhealthy'): number {
    switch (status) {
      case 'healthy':
        return HttpStatus.OK;
      case 'degraded':
        return HttpStatus.OK; // Still operational but with warnings
      case 'unhealthy':
        return HttpStatus.SERVICE_UNAVAILABLE;
      default:
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
} 