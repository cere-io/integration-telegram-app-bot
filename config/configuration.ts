export default () => ({
  telegram: {
    botToken: process.env.TELEGRAM_BOT_TOKEN,
    webhookUrl: process.env.TELEGRAM_WEBHOOK_URL,
    webhookToken: process.env.TELEGRAM_WEBHOOK_TOKEN,
    maxConnections: parseInt(process.env.TELEGRAM_WEBHOOK_MAX_CONNECTIONS) || 80,
  },
  event: {
    serviceUrl: process.env.EVENT_SERVICE_URL || 'https://ai-event.stage.cere.io',
    appId: process.env.EVENT_APP_ID,
    accountId: process.env.EVENT_ACCOUNT_ID,
    signature: process.env.EVENT_SIGNATURE,
    id: process.env.EVENT_ID,
    type: process.env.EVENT_TYPE || 'TELEGRAM_MESSAGE',
    timestamp: process.env.EVENT_TIMESTAMP,
    dataServiceId: process.env.EVENT_DATA_SERVICE_ID,
    userPubKey: process.env.EVENT_USER_PUB_KEY,
  },
  app: {
    port: parseInt(process.env.PORT) || 8080,
    nodeEnv: process.env.NODE_ENV || 'development',
    version: process.env.APP_VERSION || '1.0.0',
  },
  
  unified: {
    enableBatching: process.env.UNIFIED_ENABLE_BATCHING === 'true' || true,
    defaultBatchSize: parseInt(process.env.UNIFIED_DEFAULT_BATCH_SIZE) || 100,
    defaultBatchTimeout: parseInt(process.env.UNIFIED_DEFAULT_BATCH_TIMEOUT) || 5000,
    maxRetries: parseInt(process.env.UNIFIED_MAX_RETRIES) || 3,
    retryDelay: parseInt(process.env.UNIFIED_RETRY_DELAY) || 1000,
    logLevel: process.env.UNIFIED_LOG_LEVEL || 'info',
    enableMetrics: process.env.UNIFIED_ENABLE_METRICS === 'true' || true,
    writeMode: process.env.UNIFIED_WRITE_MODE || 'realtime',
    encryption: process.env.UNIFIED_ENCRYPTION === 'true' || false,
    healthCheckInterval: parseInt(process.env.UNIFIED_HEALTH_CHECK_INTERVAL) || 30000,
  },
}); 