# Phase 3: Telegram Bot Integration

## Objectives
- Implement Telegram Bot API service
- Create webhook registration system
- Setup webhook controller with authentication
- Implement message filtering and routing

## Prerequisites from Phase 2
- ✅ Configuration system (`configuration.ts`, interfaces)
- ✅ HTTP infrastructure (`BaseHttpService`, HTTP module)
- ✅ Validation service and error handling
- ✅ Logging infrastructure

## Tasks

### Task 3.1: Implement Telegram Bot Service (2 hours)
**Dependencies**: Phase 2 Task 2.3 (BaseHttpService), Task 2.1 (Configuration)

Create `src/services/telegram-bot.service.ts`:
```typescript
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
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
      allowed_updates: ['message', 'callback_query', 'chat_member'],
    };

    const response = await this.post(`${this.baseUrl}/setWebhook`, payload);
    return response.data.ok;
  }
}
```

### Task 3.2: Create Webhook Controller (1.5 hours)
**Dependencies**: Phase 2 Task 2.5 (ValidationService), Task 2.6 (ErrorHandlerService)

Create `src/controllers/webhook.controller.ts`:
```typescript
import { Controller, Post, Body, Headers, UnauthorizedException, HttpCode, HttpStatus } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ValidationService } from '../services/validation.service';
import { TelegramUpdateDto } from '../dto/telegram-update.dto';

@Controller('telegram/webhook')
export class WebhookController {
  private readonly authToken: string;
  private readonly handledTypes = new Set(['group', 'supergroup']);

  constructor(
    private configService: ConfigService,
    private validationService: ValidationService,
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

    const update = await this.validationService.validateDto(TelegramUpdateDto, rawUpdate);
    
    if (this.shouldProcessMessage(update)) {
      // Will be implemented in Phase 4
      console.log('Message ready for processing:', update.message?.text);
    }
  }

  private shouldProcessMessage(update: TelegramUpdateDto): boolean {
    const message = update.message;
    return message && 
           this.handledTypes.has(message.chat.type) && 
           this.validationService.isValidGroupMessage(message);
  }
}
```

### Task 3.3: Create Telegram DTOs (1 hour)
**Dependencies**: Phase 2 Task 2.2 (Telegram interfaces)

Create `src/dto/telegram-update.dto.ts`:
```typescript
import { IsOptional, IsNumber, IsString, IsBoolean, ValidateNested, Type } from 'class-validator';

export class TelegramUserDto {
  @IsNumber()
  id: number;

  @IsBoolean()
  is_bot: boolean;

  @IsString()
  first_name: string;

  @IsOptional()
  @IsString()
  last_name?: string;

  @IsOptional()
  @IsString()
  username?: string;
}

export class TelegramChatDto {
  @IsNumber()
  id: number;

  @IsString()
  type: string;

  @IsOptional()
  @IsString()
  title?: string;
}

export class TelegramMessageDto {
  @IsNumber()
  message_id: number;

  @IsNumber()
  date: number;

  @ValidateNested()
  @Type(() => TelegramChatDto)
  chat: TelegramChatDto;

  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramUserDto)
  from?: TelegramUserDto;

  @IsOptional()
  @IsString()
  text?: string;

  @IsOptional()
  @IsString()
  caption?: string;
}

export class TelegramUpdateDto {
  @IsNumber()
  update_id: number;

  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  message?: TelegramMessageDto;
}
```

### Task 3.4: Implement Webhook Registration Service (1 hour)
**Dependencies**: Task 3.1 (TelegramBotService), Phase 2 Task 2.4 (LoggerService)

Create `src/services/webhook.service.ts`:
```typescript
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
```

### Task 3.5: Update App Module (30 minutes)
**Dependencies**: All previous tasks in Phase 3

Update `src/app.module.ts`:
```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { HttpModule } from '@nestjs/axios';
import { WebhookController } from './controllers/webhook.controller';
import { TelegramBotService } from './services/telegram-bot.service';
import { WebhookService } from './services/webhook.service';
import { ValidationService } from './services/validation.service';
import { LoggerService } from './services/logger.service';
import { BaseHttpService } from './services/base-http.service';
import configuration from '../config/configuration';

@Module({
  imports: [
    ConfigModule.forRoot({
      load: [configuration],
      isGlobal: true,
    }),
    HttpModule,
  ],
  controllers: [WebhookController],
  providers: [
    BaseHttpService,
    TelegramBotService,
    WebhookService,
    ValidationService,
    LoggerService,
  ],
})
export class AppModule {}
```

## Validation Steps

### Validation 3.1: Webhook Registration Test
```bash
# Set environment variables and test
cp .env.example .env
# Add real TELEGRAM_BOT_TOKEN
npm run start:dev
# Expected: "Telegram webhook registered successfully"
```

### Validation 3.2: Webhook Endpoint Test
```bash
curl -X POST http://localhost:8080/telegram/webhook \
  -H "x-telegram-bot-api-secret-token: your-webhook-token" \
  -H "Content-Type: application/json" \
  -d '{"update_id": 123, "message": {"message_id": 1, "date": 1640995200, "chat": {"id": -123, "type": "group"}}}'
# Expected: 200 OK response
```

## Deliverables Summary
✅ **Telegram Bot Service**: API integration with webhook setup  
✅ **Webhook Controller**: Request handling with authentication  
✅ **Telegram DTOs**: Validated data transfer objects  
✅ **Webhook Registration**: Automatic startup registration  
✅ **Module Integration**: All services registered in AppModule  

## Next Phase Prerequisites
1. ✅ Webhook registration works with real Telegram bot token
2. ✅ Webhook endpoint accepts and validates Telegram updates
3. ✅ Message filtering correctly identifies group messages
4. ✅ All services integrate without dependency injection errors

## Dependencies for Phase 4
- **WebhookController**: Will integrate message processing service
- **ValidationService**: Will validate message structures  
- **TelegramBotService**: Provides authenticated API access
- **Telegram DTOs**: Will be extended for complete message data
- **LoggerService**: Will log message processing activities 