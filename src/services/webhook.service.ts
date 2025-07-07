import { Injectable, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { TelegramBotService } from './telegram-bot.service';
import { LoggerService } from './logger.service';

@Injectable()
export class WebhookService implements OnModuleInit {
  constructor(
    private configService: ConfigService,
    private telegramBotService: TelegramBotService,
    private loggerService: LoggerService,
  ) {}

  async onModuleInit() {
    await this.registerWebhook();
  }

  async registerWebhook(): Promise<void> {
    this.loggerService.logWebhookRegistration();
    
    const webhookUrl = this.configService.get<string>('telegram.webhookUrl');
    const webhookToken = this.configService.get<string>('telegram.webhookToken');
    const maxConnections = this.configService.get<number>('telegram.maxConnections');
    
    await this.telegramBotService.setWebhook(webhookUrl, webhookToken, maxConnections);
    
    this.loggerService.logWebhookRegistered();
  }
} 