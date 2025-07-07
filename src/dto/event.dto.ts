import { IsBoolean, IsString, IsObject, IsOptional } from 'class-validator';

export class EventPayloadDto {
  @IsString()
  message_id: string;

  @IsString()  
  group_id: string;

  @IsString()
  group_title: string;

  @IsString()
  message_text: string;

  @IsString()
  message_timestamp: string;

  @IsOptional()
  @IsObject()
  author?: {
    id: string;
    username: string;
    first_name: string;
    last_name: string;
    is_bot: boolean;
  };

  @IsOptional()
  @IsObject()
  photos?: Array<{
    file_id: string;
    file_unique_id: string;
    width: string;
    height: string;
    file_size: string;
  }>;

  @IsOptional()
  @IsObject()
  video?: {
    file_id: string;
    file_unique_id: string;
    width: string;
    height: string;
    duration: string;
  };
}

export class ActivityEventDto {
  @IsBoolean()
  generated: boolean;

  @IsBoolean()
  is_debug: boolean;

  @IsString()
  app_id: string;

  @IsString()
  connection_id: string;

  @IsString()
  session_id: string;

  @IsString()
  account_id: string;

  @IsString()
  signature: string;

  @IsString()
  id: string;

  @IsString()
  event_type: string;

  @IsString()
  timestamp: string;

  @IsObject()
  payload: EventPayloadDto;

  @IsString()
  data_service_id: string;

  @IsString()
  user_pub_key: string;
} 