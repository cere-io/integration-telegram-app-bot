// Main application entry point
// TODO: Implement in Phase 2 

import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { AppModule } from './app.module';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  const logger = new Logger('Bootstrap');
  
  // Enable CORS
  app.enableCors({
    origin: '*',
    methods: ['GET', 'POST'],
  });
  
  // Serve static files for Mini App
  app.useStaticAssets(join(process.cwd(), 'src', 'main', 'resources', 'META-INF', 'resources'), {
    index: false // Disable automatic index resolution to allow our controller to handle it
  });
  
  // Also serve files from the public directory
  app.useStaticAssets(join(process.cwd(), 'public'), {
    prefix: '/', // Serve files from the root URL path
  });
  
  // Enable validation
  app.useGlobalPipes(new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
  }));
  
  const port = process.env.PORT || 8080;
  await app.listen(port);
  logger.log(`Telegram Bot running on port ${port}`);
}

bootstrap().catch(error => {
  const logger = new Logger('Bootstrap');
  logger.error('Failed to start application:', error);
  process.exit(1);
}); 