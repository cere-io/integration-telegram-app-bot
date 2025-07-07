import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { NestFactory } from '@nestjs/core';
import { AppModule } from '../app.module';
import { TelegramBotService } from '../services/telegram-bot.service';

async function registerWebhook() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);
  const httpService = app.get(HttpService);
  
  const telegramService = new TelegramBotService(httpService, configService);
  
  const webhookUrl = configService.get<string>('telegram.webhookUrl');
  const webhookToken = configService.get<string>('telegram.webhookToken');
  const maxConnections = configService.get<number>('telegram.maxConnections', 40);
  
  try {
    // First check current webhook status
    console.log('Checking current webhook status...');
    const webhookInfo = await telegramService.getWebhookInfo();
    console.log('Current webhook info:');
    console.log(JSON.stringify(webhookInfo, null, 2));
    
    // Register the new webhook
    console.log(`\nRegistering webhook at ${webhookUrl}`);
    const result = await telegramService.setWebhook(webhookUrl, webhookToken, maxConnections);
    
    if (result) {
      console.log('Webhook registered successfully!');
      
      // Verify the new webhook settings
      const newWebhookInfo = await telegramService.getWebhookInfo();
      console.log('\nNew webhook configuration:');
      console.log(JSON.stringify(newWebhookInfo, null, 2));
      
      console.log('\nThe bot will now receive all update types including:');
      console.log('- Regular messages');
      console.log('- Edited messages');
      console.log('- Channel posts');
      console.log('- Group membership changes');
    } else {
      console.error('Failed to register webhook');
    }
  } catch (error) {
    console.error('Error registering webhook:', error);
  } finally {
    await app.close();
  }
}

registerWebhook(); 