import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { HttpModule } from '@nestjs/axios';
import { WebhookController } from './controllers/webhook.controller';
import { HealthCheckController } from './controllers/health-check.controller';
import { AppController } from './controllers/app.controller';
import { TelegramBotService } from './services/telegram-bot.service';
import { WebhookService } from './services/webhook.service';
import { MessageHandlerService } from './services/message-handler.service';
import { CryptoService } from './services/crypto.service';
import { ValidationService } from './services/validation.service';
import { LoggerService } from './services/logger.service';
import { BaseHttpService } from './services/base-http.service';
import { ErrorHandlerService } from './services/error-handler.service';
import { UnifiedSdkService } from './services/unified-sdk.service';
import { HealthCheckService } from './services/health-check.service';
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
  controllers: [WebhookController, HealthCheckController, AppController],
  providers: [
    BaseHttpService,
    TelegramBotService,
    WebhookService,
    ValidationService,
    LoggerService,
    MessageHandlerService,
    CryptoService,
    ErrorHandlerService,
    UnifiedSdkService,
    HealthCheckService,
  ],
  exports: [
    BaseHttpService,
    TelegramBotService,
    WebhookService,
    ValidationService,
    LoggerService,
    MessageHandlerService,
    CryptoService,
    ErrorHandlerService,
    UnifiedSdkService,
    HealthCheckService,
  ],
})
export class AppModule {} 