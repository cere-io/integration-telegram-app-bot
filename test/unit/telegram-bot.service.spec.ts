import { Test, TestingModule } from '@nestjs/testing';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { TelegramBotService } from '../../src/services/telegram-bot.service';
import { of } from 'rxjs';
import { AxiosResponse } from 'axios';

describe('TelegramBotService', () => {
  let service: TelegramBotService;
  let httpService: HttpService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        TelegramBotService,
        {
          provide: HttpService,
          useValue: {
            request: jest.fn(),
          },
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key: string) => {
              if (key === 'telegram.botToken') return 'test-token';
              return null;
            }),
          },
        },
      ],
    }).compile();

    service = module.get<TelegramBotService>(TelegramBotService);
    httpService = module.get<HttpService>(HttpService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should set a webhook', async () => {
    const response: AxiosResponse = { data: { ok: true }, status: 200, statusText: 'OK', headers: {}, config: {} as any };
    jest.spyOn(httpService, 'request').mockReturnValue(of(response));

    const result = await service.setWebhook('https://test.com', 'secret', 40);
    expect(result).toBe(true);
    expect(httpService.request).toHaveBeenCalledWith(
      expect.objectContaining({
        method: 'POST',
        url: 'https://api.telegram.org/bottest-token/setWebhook',
      }),
    );
  });
});
