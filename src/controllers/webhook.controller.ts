import { Controller, Post, Body, Headers, UnauthorizedException, HttpCode, HttpStatus } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ValidationService } from '../services/validation.service';
import { MessageHandlerService } from '../services/message-handler.service';
import { LoggerService } from '../services/logger.service';
import { TelegramUpdateDto } from '../dto/telegram-update.dto';

@Controller('telegram/webhook')
export class WebhookController {
  private readonly authToken: string;
  private readonly handledTypes = new Set(['group', 'supergroup']);

  constructor(
    private configService: ConfigService,
    private validationService: ValidationService,
    private messageHandlerService: MessageHandlerService,
    private loggerService: LoggerService,
  ) {
    this.authToken = this.configService.get<string>('telegram.webhookToken');
  }

  @Post()
  @HttpCode(HttpStatus.OK)
  async handleWebhook(
    @Headers('x-telegram-bot-api-secret-token') auth: string,
    @Body() rawUpdate: any,
  ): Promise<void> {
    if (auth !== this.authToken) {
      throw new UnauthorizedException('Invalid secret token');
    }

    const update = await this.validationService.validateDto(TelegramUpdateDto, rawUpdate) as TelegramUpdateDto;
    
    // Log the incoming update with detailed information
    this.loggerService.logWebhookReceived(update);
    
    // Process any message type
    if (update.message) {
      await this.processMessage(update, update.message);
    } 
    // Process edited messages
    else if (update.edited_message) {
      await this.processMessage(update, update.edited_message, 'edited');
    }
    // Process channel posts
    else if (update.channel_post) {
      await this.processMessage(update, update.channel_post, 'channel');
    }
    // Process edited channel posts
    else if (update.edited_channel_post) {
      await this.processMessage(update, update.edited_channel_post, 'edited_channel');
    }
    // Process chat member updates
    else if (update.chat_member || update.my_chat_member) {
      console.log('Received chat member update');
      // Handle chat member updates if needed
    }
  }
  
  private async processMessage(update: TelegramUpdateDto, message: any, messageType: string = 'regular'): Promise<void> {
    // Check if this is a group/supergroup message
    if (message && message.chat && this.handledTypes.has(message.chat.type)) {
      // Skip bot commands and replies to bot messages
      if (message?.text?.startsWith('/') || message?.reply_to_message?.from?.is_bot) {
        this.loggerService.log('debug', `Skipping ${messageType} message: bot command or reply to bot`);
        return;
      }

      try {
        const event = await this.messageHandlerService.handle(update);
        this.loggerService.log('log', `Processed ${messageType} message successfully`);
      } catch (error) {
        this.loggerService.log('error', `Error processing ${messageType} message:`, error);
      }
    }
  }
} 