import { IsOptional, IsNumber, IsString, IsBoolean, ValidateNested } from 'class-validator';
import { Type } from 'class-transformer';

export class TelegramPhotoDto {
  @IsString()
  file_id: string;

  @IsString()
  file_unique_id: string;

  @IsNumber()
  width: number;

  @IsNumber()
  height: number;

  @IsOptional()
  @IsNumber()
  file_size?: number;
}

export class TelegramVideoDto {
  @IsString()
  file_id: string;

  @IsString()
  file_unique_id: string;

  @IsNumber()
  width: number;

  @IsNumber()
  height: number;

  @IsNumber()
  duration: number;
}

export class TelegramUserDto {
  @IsNumber()
  id: number;

  @IsBoolean()
  is_bot: boolean;

  @IsString()
  first_name: string;

  @IsOptional()
  @IsString()
  last_name?: string;

  @IsOptional()
  @IsString()
  username?: string;
}

export class TelegramChatDto {
  @IsNumber()
  id: number;

  @IsString()
  type: string;

  @IsOptional()
  @IsString()
  title?: string;
}

export class TelegramMessageDto {
  @IsNumber()
  message_id: number;

  @IsNumber()
  date: number;

  @ValidateNested()
  @Type(() => TelegramChatDto)
  chat: TelegramChatDto;

  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramUserDto)
  from?: TelegramUserDto;

  @IsOptional()
  @IsString()
  text?: string;

  @IsOptional()
  @IsString()
  caption?: string;

  @IsOptional()
  @ValidateNested({ each: true })
  @Type(() => TelegramPhotoDto)
  photo?: TelegramPhotoDto[];

  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramVideoDto)
  video?: TelegramVideoDto;

  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  reply_to_message?: TelegramMessageDto;
}

export class TelegramUpdateDto {
  @IsNumber()
  update_id: number;

  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  message?: TelegramMessageDto;
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  edited_message?: TelegramMessageDto;
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  channel_post?: TelegramMessageDto;
  
  @IsOptional()
  @ValidateNested()
  @Type(() => TelegramMessageDto)
  edited_channel_post?: TelegramMessageDto;
  
  @IsOptional()
  @ValidateNested()
  chat_member?: any;
  
  @IsOptional()
  @ValidateNested()
  my_chat_member?: any;
} 