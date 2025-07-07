export interface TelegramConfig {
  botToken: string;
  webhookUrl: string;
  webhookToken: string;
  maxConnections: number;
}

export interface EventConfig {
  serviceUrl: string;
  appId: string;
  accountId: string;
  signature: string;
  id: string;
  type: string;
  timestamp: string;
  dataServiceId: string;
  userPubKey: string;
}

export interface AppConfig {
  port: number;
  nodeEnv: string;
}

export interface ApplicationConfiguration {
  telegram: TelegramConfig;
  event: EventConfig;
  app: AppConfig;
} 