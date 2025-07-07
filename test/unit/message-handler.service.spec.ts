import { Test, TestingModule } from '@nestjs/testing';
import { MessageHandlerService } from '../../src/services/message-handler.service';
import { ConfigService } from '@nestjs/config';
import { CryptoService } from '../../src/services/crypto.service';
import { LoggerService } from '../../src/services/logger.service';
import { UnifiedSdkService } from '../../src/services/unified-sdk.service';
import { TelegramUpdateDto } from '../../src/dto/telegram-update.dto';

describe('MessageHandlerService', () => {
  let service: MessageHandlerService;
  let unifiedSdkService: UnifiedSdkService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        MessageHandlerService,
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string) => {
              if (key === 'event.appId') return 'test-app-id';
              if (key === 'unified.writeMode') return 'realtime';
              if (key === 'unified.encryption') return false;
              return null;
            }),
          },
        },
        {
          provide: CryptoService,
          useValue: {
            generateAccountId: jest.fn().mockResolvedValue('mock-account-id'),
            toLongValue: jest.fn((val) => val),
          },
        },
        {
          provide: LoggerService,
          useValue: {
            logMessageProcessing: jest.fn(),
            logEventSent: jest.fn(),
            logEventFailed: jest.fn(),
          },
        },
        {
          provide: UnifiedSdkService,
          useValue: {
            sendTelegramEvent: jest.fn().mockResolvedValue({ transactionId: 'mock-tx-id' }),
          },
        },
      ],
    }).compile();

    service = module.get<MessageHandlerService>(MessageHandlerService);
    unifiedSdkService = module.get<UnifiedSdkService>(UnifiedSdkService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should handle a simple text message and call the Unified SDK', async () => {
    const update = {
      update_id: 1,
      message: {
        message_id: 101,
        date: 1672531200,
        chat: { id: -12345, type: 'group', title: 'Test Group' },
        from: { id: 98765, is_bot: false, first_name: 'Test', last_name: 'User', username: 'testuser' },
        text: 'Hello, world!',
      },
    } as unknown as TelegramUpdateDto;

    await service.handle(update);

    expect(unifiedSdkService.sendTelegramEvent).toHaveBeenCalled();
    const sdkCall = (unifiedSdkService.sendTelegramEvent as jest.Mock).mock.calls[0];
    const eventPayload = sdkCall[0].payload;

    expect(eventPayload.message_id).toBe(101);
    expect(eventPayload.group_title).toBe('Test Group');
    expect(eventPayload.message_text).toBe('Hello, world!');
    expect(eventPayload.author.id).toBe('98765');
  });
});
