import { Test, TestingModule } from '@nestjs/testing';
import { ValidationService } from '../../src/services/validation.service';
import { TelegramUpdateDto } from '../../src/dto/telegram-update.dto';

describe('ValidationService', () => {
  let service: ValidationService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [ValidationService],
    }).compile();

    service = module.get<ValidationService>(ValidationService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('validateDto', () => {
    it('should successfully validate a correct DTO', async () => {
      const validUpdate = { update_id: 1, message: { message_id: 1, date: 1, chat: { id: 1, type: 'group' } } };
      await expect(service.validateDto(TelegramUpdateDto, validUpdate)).resolves.toBeInstanceOf(Object);
    });

    it('should throw an error for an invalid DTO', async () => {
      const invalidUpdate = { update_id: 'not-a-number' };
      await expect(service.validateDto(TelegramUpdateDto, invalidUpdate)).rejects.toThrow();
    });
  });

  describe('isValidGroupMessage', () => {
    it('should return true for a valid group message', () => {
      const message = { chat: { type: 'group' } };
      expect(service.isValidGroupMessage(message)).toBe(true);
    });

    it('should return false for a private message', () => {
      const message = { chat: { type: 'private' } };
      expect(service.isValidGroupMessage(message)).toBe(false);
    });

    it('should return false for a bot command', () => {
      const message = { chat: { type: 'group' }, text: '/start' };
      expect(service.isValidGroupMessage(message)).toBe(false);
    });
  });
});
