import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { v4 as uuidv4 } from 'uuid';
import { TelegramUpdateDto } from '../dto/telegram-update.dto';
import { ActivityEventDto, EventPayloadDto } from '../dto/event.dto';
import { CryptoService } from './crypto.service';
import { LoggerService } from './logger.service';
import { UnifiedSdkService } from './unified-sdk.service';
import { TelegramUnifiedResponse, TelegramEventOptions } from '../interfaces/unified-sdk.interfaces';

@Injectable()
export class MessageHandlerService {
  constructor(
    private configService: ConfigService,
    private cryptoService: CryptoService,
    private loggerService: LoggerService,
    private unifiedSdkService: UnifiedSdkService,
  ) {}

  async handle(update: TelegramUpdateDto): Promise<TelegramUnifiedResponse> {
    // Find the first available message type in the update
    const message = update.message || 
                   update.edited_message || 
                   update.channel_post || 
                   update.edited_channel_post;
                   
    if (!message) {
      throw new Error('No message in update');
    }

    const chat = message.chat;
    const humanReadableMessage = this.createHumanReadableMessage(message, chat);
    const payload = this.createMessagePayload(message, chat);
    
    // Handle long value conversion exactly like Kotlin .longValue
    const userId = message.from ? this.cryptoService.toLongValue(message.from.id) : null;
    const accountId = userId 
      ? await this.cryptoService.generateAccountId(userId)
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

    this.loggerService.logMessageProcessing(humanReadableMessage, event, 'Event created for Unified SDK');

    // Send event through Unified SDK
    const eventOptions: TelegramEventOptions = {
      priority: this.determinePriority(message),
      writeMode: this.configService.get<'realtime' | 'batch'>('unified.writeMode', 'realtime'),
      encryption: this.configService.get<boolean>('unified.encryption', false),
      metadata: {
        traceId: uuidv4(),
        userContext: {
          messageType: this.getMessageType(message),
          chatType: chat.type,
          hasMedia: !!(message.photo || message.video),
        },
      },
    };

    try {
      const response = await this.unifiedSdkService.sendTelegramEvent(event, eventOptions);
      this.loggerService.logEventSent(200); // Success status
      return response;
    } catch (error) {
      this.loggerService.logEventFailed(error);
      throw error;
    }
  }

  private determinePriority(message: any): 'low' | 'normal' | 'high' {
    // Bot commands get high priority
    if (message.text?.startsWith('/')) {
      return 'high';
    }
    
    // Media messages get normal priority
    if (message.photo || message.video) {
      return 'normal';
    }
    
    // Regular text messages get normal priority
    return 'normal';
  }

  private getMessageType(message: any): string {
    if (message.text) return 'text';
    if (message.photo) return 'photo';
    if (message.video) return 'video';
    return 'unknown';
  }

  private createHumanReadableMessage(message: any, chat: any): string {
    let result = `New message in group ${chat.title}:\n\n`;
    
    if (message.from) {
      result += `From: ${message.from.first_name}`;
      if (message.from.last_name) result += ` ${message.from.last_name}`;
      if (message.from.username) result += ` (@${message.from.username})`;
      result += '\n\n';
    }
    
    result += this.getMessageContent(message);
    return result;
  }

  private createMessagePayload(message: any, chat: any): EventPayloadDto {
    // Handle long value conversions exactly like Kotlin
    const messageId = this.cryptoService.toLongValue(message.message_id);
    const groupId = this.cryptoService.toLongValue(chat.id);
    
    const payload: any = {
      message_id: messageId,
      group_id: groupId,
      group_title: chat.title || 'Unknown Group',
      message_text: this.getMessageContent(message),
      message_timestamp: message.date,
    };

    // Author information with proper long value handling
    if (message.from) {
      const authorId = this.cryptoService.toLongValue(message.from.id);
      payload.author = {
        id: authorId.toString(),
        username: message.from.username || 'unknown',
        first_name: message.from.first_name,
        last_name: message.from.last_name || '',
        is_bot: message.from.is_bot,
      };
    }

    // Photo array handling - exactly matching Kotlin structure
    if (message.photo && Array.isArray(message.photo)) {
      payload.photos = message.photo.map((photo: any) => ({
        file_id: photo.file_id,
        file_unique_id: photo.file_unique_id,
        width: photo.width.toString(),
        height: photo.height.toString(),
        file_size: (photo.file_size || 0).toString(),
      }));
    }

    // Video handling - exactly matching Kotlin structure
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
    // Exact matching logic from Kotlin implementation
    if (message.text) return message.text;
    if (message.caption) return message.caption;
    if (message.photo !== null && message.photo !== undefined) return '[Photo]';
    if (message.video !== null && message.video !== undefined) return '[Video]';
    if (message.document !== null && message.document !== undefined) return '[Document]';
    if (message.sticker !== null && message.sticker !== undefined) return '[Sticker]';
    return '[Unsupported message type]';
  }
} 