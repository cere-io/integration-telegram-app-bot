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

DDC_BUCKET=<DDC bucket with videos>
DDC_WALLET_MNEMONIC=<DDC wallet mnemonic>
DDC_WALLET_ALGORITHM=<SR_25519 or ED_25519>

TON_API_TOKEN=<get from @tonapibot>
TON_API_URL=https://toncenter.com/api/v2
TON_WALLET_BOUNCEABLE=<address to retrieve payments, detect here https://toncenter.com/api/v2/#/accounts/detect_address_detectAddress_get>
TON_WALLET_NONBOUNCEABLE=<address to retrieve payments, detect here https://toncenter.com/api/v2/#/accounts/detect_address_detectAddress_get>
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