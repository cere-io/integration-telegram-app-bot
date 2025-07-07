export interface TelegramUser {
  id: number;
  is_bot: boolean;
  first_name: string;
  last_name?: string;
  username?: string;
}

export interface TelegramChat {
  id: number;
  type: string;
  title?: string;
}

// Explicit photo structure matching Kotlin PhotoSize
export interface TelegramPhoto {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  file_size?: number;
}

// Explicit video structure matching Kotlin Video
export interface TelegramVideo {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
  duration: number;
}

// Document structure for completeness
export interface TelegramDocument {
  file_id: string;
  file_unique_id: string;
  file_name?: string;
  mime_type?: string;
  file_size?: number;
}

// Sticker structure for completeness
export interface TelegramSticker {
  file_id: string;
  file_unique_id: string;
  width: number;
  height: number;
}

export interface TelegramMessage {
  message_id: number;
  date: number;
  from?: TelegramUser;
  chat: TelegramChat;
  text?: string;
  caption?: string;
  // Photo is ALWAYS an array in Telegram API (different sizes of same photo)
  photo?: TelegramPhoto[];
  video?: TelegramVideo;
  document?: TelegramDocument;
  sticker?: TelegramSticker;
  reply_to_message?: TelegramMessage;
}

export interface TelegramUpdate {
  update_id: number;
  message?: TelegramMessage;
  edited_message?: TelegramMessage;
  channel_post?: TelegramMessage;
  edited_channel_post?: TelegramMessage;
  callback_query?: any;
  chat_member?: any;
  my_chat_member?: any;
}

export interface WebhookSetupRequest {
  url: string;
  max_connections: number;
  drop_pending_updates: boolean;
  secret_token: string;
  // Support all Telegram update types
  allowed_updates: string[];
} 