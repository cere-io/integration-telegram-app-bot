# Telegram Streaming Bot

## Running the application in dev mode

You will need JDK 21 or higher. Download at <https://adoptium.net/marketplace/> or use package manager to install.

Register your bot with [@BotFather](https://t.me/BotFather):
- Call `/newbot` command
- Give a name to your bot (something like `DEV Sergey's Bot`)
- Choose username for bot (something like `DEV_Sergeys_Bot`)
- Copy bot token and save in safe place

This bot uses webhook to retrieve updates from Telegram, so you need to expose your local server to the internet.
Configure one of the tunnel solutions to work with your local 8080 port:
- https://ngrok.com
- https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/

Once configured, set your local env variables.
```shell script
cp .env.example .env
```
Fill `.env` with the proper values
```shell script
TELEGRAM_BOT_TOKEN=<token from @BotFather>
TELEGRAM_WEBHOOK_URL=<your tunnel URL>
TELEGRAM_WEBHOOK_TOKEN=<auth token for your webhook endpoint>

AGENT_SERVICE_APP_ID=<agent service ID from ROB>
AGENT_SERVICE_PRIVATE_KEY=<agent service private key in HEX format>
AGENT_SERVICE_GROUP__<YOUR_TEST_GROUP_NAME>__GROUP_ID=<Telegram group id>
AGENT_SERVICE_GROUP__<YOUR_TEST_GROUP_NAME>__ORG_ID=<organization ID from ROB for this group>
AGENT_SERVICE_GROUP__<YOUR_TEST_GROUP_NAME>__CAMPAIGN_ID=<campaign ID from ROB for this group>

COMPUTE_ENGINE_URL=<compute engine endpoint>

CERE_WALLET_URL=<Cere Wallet API endpoint>
CERE_WALLET_TOKEN=<Cere Wallet API S2S token>
```

Run your application in dev mode:

```shell script
./gradlew quarkusDev
```

Register Mini App for bot:
- Go to Bot Settings in @BotFather
- Press 'Configure Mini App' button
- Provide information about app (you can serve app from the same URL as a bot's one - just copy static resources into `src/main/resources/META-INF/resources` folder)
- Your app will be available by direct URL `https://t.me/<bot>/<app>`
- Optionally you can configure `Menu Button` for bot to open Mini App by URL above

## Features

### 1. Message Onboarding (Webhook)
- Captures group messages and sends them to Compute Engine as events
- Requires group configuration in `.env` file

### 2. Campaign Management (Private Messages) 
- Handles `/start` commands with deep links
- Shows campaign information and provides WebApp access
- Dynamic campaign discovery via ROB API

### 3. Payload Processing API
- **Dynamic Group Discovery** - No manual configuration needed!
- Automatically sends notifications to groups the bot is subscribed to
- Supports 14+ data types with intelligent formatting
- RESTful API for external systems integration

#### Quick Payload API Test:
```bash
# Add bot to a Telegram group first, then:
curl -X POST http://localhost:8080/api/payload/send-json \
  -H "Content-Type: application/json" \
  -d '"🤖 Hello from the Payload API!"' \
  -G -d "source=test-system"
```

See `PAYLOAD_API1.md` for complete API documentation.