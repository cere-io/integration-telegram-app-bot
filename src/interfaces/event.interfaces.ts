export interface EventPayload {
  message_id: number;
  group_id: number;
  group_title: string;
  message_text: string;
  message_timestamp: number;
  author?: {
    id: string;
    username: string;
    first_name: string;
    last_name: string;
    is_bot: boolean;
  };
  photos?: Array<{
    file_id: string;
    file_unique_id: string;
    width: string;
    height: string;
    file_size: string;
  }>;
  video?: {
    file_id: string;
    file_unique_id: string;
    width: string;
    height: string;
    duration: string;
  };
}

export interface ActivityEvent {
  generated: boolean;
  is_debug: boolean;
  app_id: string;
  connection_id: string;
  session_id: string;
  account_id: string;
  signature: string;
  id: string;
  event_type: string;
  timestamp: string;
  payload: EventPayload;
  data_service_id: string;
  user_pub_key: string;
} 