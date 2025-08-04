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

# Static group configuration (legacy)
AGENT_SERVICE_GROUP__<YOUR_TEST_GROUP_NAME>__GROUP_ID=<Telegram group id>
AGENT_SERVICE_GROUP__<YOUR_TEST_GROUP_NAME>__ORG_ID=<organization ID from ROB for this group>
AGENT_SERVICE_GROUP__<YOUR_TEST_GROUP_NAME>__CAMPAIGN_ID=<campaign ID from ROB for this group>

# Dynamic group configuration (new)
AGENT_SERVICE_ALLOWED_ORG_IDS=<comma-separated list of organization IDs>

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

## Dynamic Groups Configuration

The bot now supports dynamic group configuration through campaign `formData`. This allows organizations to configure Telegram groups through the UI instead of static environment variables.

### Benefits
- **Multi-organization support**: One bot can serve multiple organizations
- **UI Configuration**: Groups can be configured through the campaign management UI
- **No Restarts**: Group changes don't require bot restarts
- **Campaign-Specific**: Different campaigns can have different group sets
- **Real-time Updates**: Group configurations update when campaigns are modified

### How it works
1. **Configure allowed organizations** in `AGENT_SERVICE_ALLOWED_ORG_IDS`
2. **Organizations create campaigns** in ROB
3. **Add groups to campaign formData** through the UI
4. **Bot automatically detects** groups and uses correct `orgId` and `campaignId`

### Documentation
- [Dynamic Groups Feature](DYNAMIC_GROUPS_FEATURE.md) - Technical details and implementation