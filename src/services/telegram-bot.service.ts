import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { HttpService } from '@nestjs/axios';
import { BaseHttpService } from './base-http.service';
import { WebhookSetupRequest } from '../interfaces/telegram.interfaces';

@Injectable()
export class TelegramBotService extends BaseHttpService {
  private readonly botToken: string;
  private readonly baseUrl: string;

  constructor(
    httpService: HttpService,
    private configService: ConfigService,
  ) {
    super(httpService);
    this.botToken = this.configService.get<string>('telegram.botToken');
    this.baseUrl = `https://api.telegram.org/bot${this.botToken}`;
  }

  async setWebhook(url: string, secretToken: string, maxConnections: number): Promise<boolean> {
    const payload: WebhookSetupRequest = {
      url,
      max_connections: maxConnections,
      drop_pending_updates: true,
      secret_token: secretToken,
      allowed_updates: [
        'message',
        'edited_message',
        'channel_post',
        'edited_channel_post',
        'callback_query',
        'chat_member',
        'my_chat_member'
      ],
    };

    const response = await this.post(`${this.baseUrl}/setWebhook`, payload);
    return (response.data as { ok: boolean }).ok;
  }
  
  async getWebhookInfo(): Promise<any> {
    const response = await this.get(`${this.baseUrl}/getWebhookInfo`);
    return response.data;
  }
} 