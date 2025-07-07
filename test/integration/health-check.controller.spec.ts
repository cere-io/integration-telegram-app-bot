import { Test, TestingModule } from '@nestjs/testing';
import { HealthCheckController } from '../../src/controllers/health-check.controller';
import { HealthCheckService } from '../../src/services/health-check.service';

describe('HealthCheckController', () => {
  let controller: HealthCheckController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [HealthCheckController],
      providers: [
        {
          provide: HealthCheckService,
          useValue: {
            getSystemHealth: jest.fn().mockResolvedValue({ status: 'healthy' }),
            getReadinessCheck: jest.fn().mockResolvedValue({ ready: true }),
            getLivenessCheck: jest.fn().mockResolvedValue({ alive: true }),
          },
        },
      ],
    }).compile();

    controller = module.get<HealthCheckController>(HealthCheckController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });
});
