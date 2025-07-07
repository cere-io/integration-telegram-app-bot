import { Test, TestingModule } from '@nestjs/testing';
import { WebhookController } from '../../src/controllers/webhook.controller';
import { MessageHandlerService } from '../../src/services/message-handler.service';
import { ValidationService } from '../../src/services/validation.service';
import { LoggerService } from '../../src/services/logger.service';
import { ConfigService } from '@nestjs/config';
import { TelegramUpdateDto } from '../../src/dto/telegram-update.dto';

describe('WebhookController', () => {
  let controller: WebhookController;
  let messageHandlerService: MessageHandlerService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [WebhookController],
      providers: [
        {
          provide: MessageHandlerService,
          useValue: {
            handle: jest.fn().mockResolvedValue({}),
          },
        },
        {
          provide: ValidationService,
          useValue: {
            validateDto: jest.fn((dto, data) => Promise.resolve(data)),
          },
        },
        {
          provide: LoggerService,
          useValue: {
            logWebhookReceived: jest.fn(),
            log: jest.fn(),
          },
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn().mockReturnValue('test-token'),
          },
        },
      ],
    }).compile();

    controller = module.get<WebhookController>(WebhookController);
    messageHandlerService = module.get<MessageHandlerService>(MessageHandlerService);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('should handle a valid webhook update', async () => {
    const update = {
      update_id: 1,
      message: {
        message_id: 101,
        date: 1672531200,
        chat: { id: -12345, type: 'group', title: 'Test Group' },
        from: { id: 98765, is_bot: false, first_name: 'Test', last_name: 'User', username: 'testuser' },
        text: 'Hello, world!',
      },
    } as TelegramUpdateDto;

    await controller.handleWebhook('test-token', update);
    expect(messageHandlerService.handle).toHaveBeenCalledWith(update);
  });

  it('should throw an error for an invalid token', async () => {
    const update = {} as TelegramUpdateDto;
    await expect(controller.handleWebhook('invalid-token', update)).rejects.toThrow('Invalid secret token');
  });
});
