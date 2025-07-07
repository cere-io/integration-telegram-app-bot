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