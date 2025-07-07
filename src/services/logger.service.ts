import { Injectable, Logger, LogLevel } from '@nestjs/common';

@Injectable()
export class LoggerService {
  private readonly logger = new Logger(LoggerService.name);

  logMessageProcessing(
    humanReadableMessage: string,
    event: any,
    statusMessage: string
  ): void {
    const debugInfo = `
=== Message Processing Debug Info ===

Human Readable:
${humanReadableMessage}

DDC Event:
${JSON.stringify(event, null, 2)}

Activity SDK Status:
${statusMessage}
===================================`;

    this.logger.log(debugInfo);
  }

  logWebhookReceived(update: any): void {
    this.logger.log(`Webhook received - Update ID: ${update.update_id}`);
    
    // Determine update type
    let updateType = 'unknown';
    if (update.message) updateType = 'message';
    else if (update.edited_message) updateType = 'edited_message';
    else if (update.channel_post) updateType = 'channel_post';
    else if (update.edited_channel_post) updateType = 'edited_channel_post';
    else if (update.chat_member) updateType = 'chat_member';
    else if (update.my_chat_member) updateType = 'my_chat_member';
    
    this.logger.log(`Update type: ${updateType}`);
    
    // Log message content if available
    const message = update.message || update.edited_message || update.channel_post || update.edited_channel_post;
    if (message) {
      this.logger.log(`Chat type: ${message.chat?.type || 'unknown'}`);
      
      if (message.text) this.logger.log(`Text: ${message.text}`);
      if (message.photo) this.logger.log(`Media: Photo`);
      if (message.video) this.logger.log(`Media: Video`);
      
      if (message.from) {
        this.logger.log(`From: ${message.from.first_name} ${message.from.last_name || ''} (${message.from.id})`);
      }
    }
  }

  logEventSent(status: number): void {
    this.logger.debug(`Event sent successfully (status: ${status})`);
  }

  logEventFailed(error: any): void {
    this.logger.error('Failed to send event to Activity SDK', error);
  }

  logWebhookRegistration(): void {
    this.logger.log('Registering Telegram webhook');
  }

  logWebhookRegistered(): void {
    this.logger.log('Telegram webhook registered successfully');
  }

  logUnauthorizedWebhook(): void {
    this.logger.warn('Unauthorized webhook request - token mismatch');
  }

  log(logLevel: string, message: string, error?: any): void {

    // Log message accoridng to the log levels 
    switch (logLevel) {
        case "log":
            this.logger.log(message)
            break;
        case "debug":
            this.logger.verbose(message)
            break;
        case "error":
            this.logger.error(`Error Message: ${MessageChannel} \n Error: ${error}`)
        default:
            this.logger.log(message)
            break;
    }


  }
} 