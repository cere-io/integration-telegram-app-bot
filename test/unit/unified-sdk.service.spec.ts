import { Test, TestingModule } from '@nestjs/testing';
import { UnifiedSdkService } from '../../src/services/unified-sdk.service';
import { ConfigService } from '@nestjs/config';
import { LoggerService } from '../../src/services/logger.service';
import { CryptoService } from '../../src/services/crypto.service';
import { ErrorHandlerService } from '../../src/services/error-handler.service';

// Mock the entire UnifiedSDK module
jest.mock('../../src/unified-sdk/src/UnifiedSDK', () => {
  return {
    UnifiedSDK: jest.fn().mockImplementation(() => {
      return {
        initialize: jest.fn().mockResolvedValue(undefined),
        writeData: jest.fn().mockResolvedValue({ transactionId: 'mock-tx-id' }),
        cleanup: jest.fn().mockResolvedValue(undefined),
        getStatus: jest.fn().mockReturnValue({ initialized: true }),
      };
    }),
  };
});

describe('UnifiedSdkService', () => {
  let service: UnifiedSdkService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UnifiedSdkService,
        { provide: ConfigService, useValue: { get: jest.fn() } },
        { provide: LoggerService, useValue: { log: jest.fn(), error: jest.fn() } },
        { provide: CryptoService, useValue: {} },
        { provide: ErrorHandlerService, useValue: {} },
      ],
    }).compile();

    service = module.get<UnifiedSdkService>(UnifiedSdkService);
    await service.onModuleInit();
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should initialize the UnifiedSDK on module init', () => {
    expect(service.isInitialized()).toBe(true);
  });
});
