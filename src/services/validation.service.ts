import { Injectable, BadRequestException } from '@nestjs/common';
import { validate, ValidationError } from 'class-validator';
import { plainToClass } from 'class-transformer';

@Injectable()
export class ValidationService {
  async validateDto<T extends object>(
    dtoClass: new () => T,
    data: any
  ): Promise<T> {
    const dto = plainToClass(dtoClass, data);
    const errors = await validate(dto);

    if (errors.length > 0) {
      const errorMessages = this.formatValidationErrors(errors);
      throw new BadRequestException({
        message: 'Validation failed',
        errors: errorMessages,
      });
    }

    return dto;
  }

  private formatValidationErrors(errors: ValidationError[]): string[] {
    return errors.flatMap(error => 
      Object.values(error.constraints || {})
    );
  }

  isValidTelegramUpdate(data: any): boolean {
    return (
      data &&
      typeof data.update_id === 'number' &&
      (data.message || 
       data.edited_message || 
       data.channel_post || 
       data.edited_channel_post || 
       data.callback_query || 
       data.chat_member || 
       data.my_chat_member)
    );
  }

  isValidGroupMessage(message: any): boolean {
    return (
      message &&
      message.chat &&
      ['group', 'supergroup'].includes(message.chat.type) &&
      !message.text?.startsWith('/')
    );
  }
}
 