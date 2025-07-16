# Payload Processing API

The Telegram Bot now includes a powerful payload processing API that can receive various data types, analyze them automatically, and send formatted messages to configured Telegram chats (both group chats and private chats).

## 🚀 Quick Start

### Send a Simple Text Message
```bash
curl -X POST http://localhost:8080/api/payload/send-json \
  -H "Content-Type: application/json" \
  -d '"Hello from the API!"' \
  -G -d "source=external-system"
```

### Send Structured Data
```bash
curl -X POST http://localhost:8080/api/payload/send \
  -H "Content-Type: application/json" \
  -d '{
    "payload": {
      "user": "John Doe",
      "score": 95.5,
      "active": true
    },
    "source": "analytics-system",
    "priority": "HIGH"
  }'
```

## 📋 API Endpoints

### `POST /api/payload/send`
Main endpoint for sending structured payload requests.

**Request Body:**
```json
{
  "payload": any,              // The actual data (any JSON type)
  "source": "string",          // Optional: Source system identifier
  "metadata": {},              // Optional: Additional metadata
  "target_chats": ["-1001234567890", "123456789"], // Optional: Specific chat IDs (groups: negative, private: positive)
  "priority": "NORMAL"         // LOW, NORMAL, HIGH, URGENT
}
```

**Response:**
```json
{
  "success": true,
  "message": "Payload processed and sent to 2/2 chats",
  "processedType": "JSON_OBJECT",
  "sentToChatCount": 2,
  "errors": [],
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### `POST /api/payload/send-json`
Simplified endpoint for quick JSON payload sending.

**Query Parameters:**
- `source` (optional): Source system name
- `priority` (optional): Message priority level
- `target_chats` (optional): Comma-separated chat IDs

**Request Body:** Any JSON value (string, number, object, array, etc.)

### `GET /api/payload/health`
Health check endpoint.

### `GET /api/payload/types`
List all supported payload types with descriptions.

## 🎯 Supported Data Types

The system automatically detects and formats the following data types:

| Type | Example | Formatted Output |
|------|---------|------------------|
| **Text** | `"Hello World"` | 📝 **Text Message**<br/>Hello World |
| **Number** | `42.5` | 🔢 **Decimal Number**<br/>`42.5` |
| **Boolean** | `true` | ✅ **Boolean Value**<br/>**Result:** TRUE |
| **URL** | `"https://example.com"` | 🔗 **URL Received**<br/>[https://example.com](https://example.com) |
| **Email** | `"user@example.com"` | 📧 **Email Address**<br/>`user@example.com` |
| **Phone** | `"+1-555-123-4567"` | 📞 **Phone Number**<br/>`+1-555-123-4567` |
| **Currency** | `"$99.99"` | 💰 **Currency Amount**<br/>**Amount:** $99.99 |
| **Percentage** | `"85.5%"` | 📊 **Percentage**<br/>**Value:** 85.5% |
| **Date/Time** | `"2024-01-01T12:00:00Z"` | 📅 **Date/Time**<br/>**Parsed:** 2024-01-01 12:00:00 |
| **JSON Object** | `{"name": "John"}` | 🏗️ **JSON Object**<br/>```json<br/>{"name": "John"}<br/>``` |
| **JSON Array** | `[1, 2, 3]` | 📋 **JSON Array**<br/>```json<br/>[1, 2, 3]<br/>``` |
| **Base64 Image** | `"data:image/png;base64,..."` | 🖼️ **Image Received**<br/>*Sends image with metadata* |

## 🎨 Message Formatting

Messages are automatically formatted with:
- ✨ **Rich formatting** with emojis and Markdown
- 🏷️ **Type indicators** showing detected data type
- 📊 **Metadata information** for complex types
- 🚨 **Priority indicators** for urgent messages
- 📝 **Source attribution** when provided
## 💡 Usage Examples

### 1. System Alerts
```bash
curl -X POST http://localhost:8080/api/payload/send \
  -H "Content-Type: application/json" \
  -d '{
    "payload": "Database connection restored",
    "source": "monitoring-system",
    "priority": "HIGH"
  }'
```

### 2. Analytics Data
```bash
curl -X POST http://localhost:8080/api/payload/send \
  -H "Content-Type: application/json" \
  -d '{
    "payload": {
      "daily_users": 1250,
      "conversion_rate": "3.2%",
      "revenue": "$15,750.00"
    },
    "source": "analytics-dashboard"
  }'
```

### 3. User Metrics
```bash
curl -X POST http://localhost:8080/api/payload/send-json \
  -H "Content-Type: application/json" \
  -d '[
    {"user": "alice", "score": 95},
    {"user": "bob", "score": 87},
    {"user": "charlie", "score": 92}
  ]' \
  -G -d "source=leaderboard" -d "priority=NORMAL"
```

### 4. File Upload Notification
```bash
curl -X POST http://localhost:8080/api/payload/send \
  -H "Content-Type: application/json" \
  -d '{
    "payload": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==",
    "source": "file-upload-service"
  }'
```

### 5. Send to Specific Chats (Both Group and Private)
```bash
curl -X POST http://localhost:8080/api/payload/send \
  -H "Content-Type: application/json" \
  -d '{
    "payload": "🎉 Campaign completed successfully!",
    "source": "campaign-manager",
    "priority": "HIGH",
    "target_chats": [
      "-1001234567890",
      "123456789",
      "987654321"
    ]
  }'
```

## 🎯 Chat Targeting

The API can send messages to both group chats and private chats:

### **Default Behavior**
If no `target_chats` are specified, messages are sent to **all groups the bot is currently subscribed to**. Groups are discovered **automatically** when:
- Bot receives any message from a group (indicates active subscription)
- Bot is added to a group (via `my_chat_member` updates)
- No manual configuration needed!

### **Specific Chat Targeting**
You can target specific chats using the `target_chats` parameter:

```json
{
  "target_chats": [
    "-1001234567890",  // Group/Supergroup chat (negative ID)
    "123456789",       // Private chat with user (positive ID)
    "-987654321"       // Another group chat
  ]
}
```

### **Chat ID Format**
- **Group chats**: Negative numbers (e.g., `-1001234567890`)
- **Private chats**: Positive numbers (user IDs, e.g., `123456789`)
- **Legacy groups**: Negative numbers without `-100` prefix (e.g., `-987654321`)

### **Dynamic Group Discovery**
The bot automatically tracks which groups it's subscribed to:

1. **Add bot to group** → Bot immediately starts tracking that group
2. **Send any message in group** → Confirms bot is still subscribed
3. **Remove bot from group** → Bot stops tracking that group

**Check current subscriptions:**
```bash
curl http://localhost:8080/api/payload/health
```

### **Getting Chat IDs**
- **Groups**: Invite [@userinfobot](https://t.me/userinfobot) to your group
- **Private**: Send `/start` to [@userinfobot](https://t.me/userinfobot)
- **From logs**: Check bot logs when messages are received
- **Health endpoint**: See currently tracked group IDs

## 🔍 Testing

### 1. Check Bot Status & Subscriptions
```bash
curl http://localhost:8080/api/payload/health
```
**Response shows:**
- `subscribed_groups`: Number of groups bot is subscribed to
- `subscribed_group_ids`: List of actual group IDs
- `supported_types`: Number of data types supported

### 2. View Supported Data Types
```bash
curl http://localhost:8080/api/payload/types
```

### 3. Quick Setup Test
1. **Add bot to a Telegram group**
2. **Send any message in that group** (to trigger discovery)
3. **Check health endpoint** - should show 1 subscribed group
4. **Send test payload** - should appear in the group

```bash
# Simple test message
curl -X POST http://localhost:8080/api/payload/send-json \
  -H "Content-Type: application/json" \
  -d '"🤖 Bot is working! This is a test message."' \
  -G -d "source=setup-test"
```

## 🚨 Error Handling

The API provides detailed error responses:

```json
{
  "success": false,
  "message": "Failed to send payload to any chats",
  "processedType": "JSON_OBJECT",
  "sentToChatCount": 0,
  "errors": [
    "Chat 12345: Network timeout",
    "Chat 67890: Invalid chat ID"
  ],
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## 🔒 Security Considerations

- 🛡️ **Input validation** for all payload types
- 📏 **Size limits** for messages and attachments
- 🔐 **Configuration-based** chat targeting
- 📝 **Comprehensive logging** for audit trails
- ⚡ **Rate limiting** through Quarkus configuration

## 🏗️ Architecture

The payload processing pipeline consists of:

1. **PayloadEndpoint** - REST API entry point
2. **PayloadAnalyzer** - Data type detection engine
3. **MessageFormatter** - Telegram message formatting
4. **ChatNotificationService** - Multi-chat delivery
5. **PayloadProcessingLogger** - Enhanced logging
