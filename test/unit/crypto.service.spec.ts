import { Test, TestingModule } from '@nestjs/testing';
import { CryptoService } from '../../src/services/crypto.service';

describe('CryptoService', () => {
  let service: CryptoService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [CryptoService],
    }).compile();

    service = module.get<CryptoService>(CryptoService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('generateAccountId', () => {
    it('should generate a deterministic account ID', async () => {
      const userId = 123456789;
      const accountId1 = await service.generateAccountId(userId);
      const accountId2 = await service.generateAccountId(userId);
      expect(accountId1).toEqual(accountId2);
    });

    it('should generate a valid hex account ID', async () => {
      const userId = 123456789;
      const accountId = await service.generateAccountId(userId);
      expect(accountId).toMatch(/^0x[0-9a-f]{64}$/);
    });
  });
});
