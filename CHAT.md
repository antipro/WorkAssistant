# Chat Application

This document describes the chat application feature added to WorkAssistant.

## Overview

A real-time chat application with AI integration, built using Vue.js (frontend) and Java/Javalin (backend).

## Features

### 1. Simple Login
- No password required
- Enter nickname to join chat
- Automatic private AI channel creation

### 2. Three-Panel Layout
```
┌──────────────┬─────────────────────────┬──────────────┐
│   Channels   │      Chat Area          │    Users     │
│              │                         │              │
│ 🔒 AI Private│  ┌─────────────────┐    │ • Alice      │
│ # general    │  │ Message History │    │ • Bob        │
│ # project-a  │  └─────────────────┘    │ • Charlie    │
│              │  ┌─────────────────┐    │              │
│              │  │ Input Box       │    │              │
│              │  └─────────────────┘    │              │
└──────────────┴─────────────────────────┴──────────────┘
```

### 3. Channel Management
- **Default Channels**: 
  - Private AI channel (created per user)
  - General channel (for all users)
- **Create New Channel**: Type `#channelname` in any channel
- **Switch Channels**: Click on channel in left sidebar

### 4. AI Integration
- **AI Assistant**: Named "eking"
- **Private Channel**: Direct 1-on-1 chat with AI
- **Mention in Public Channels**: Type `@eking` followed by your question
- **Automatic Response**: AI responds asynchronously in the same channel

### 5. Real-time Updates
- Polls for new messages every 2 seconds
- Updates user list in real-time
- Shows online users count

## API Endpoints

### Authentication
```
POST /api/chat/login
Body: { "nickname": "Alice" }
Response: { "user": {...}, "privateChannel": {...} }
```

### Users
```
GET /api/chat/users
Response: { "data": [{ "id": "...", "nickname": "Alice", "online": true }] }
```

### Channels
```
GET /api/chat/channels?userId=<userId>
Response: { "data": [{ "id": "...", "name": "general", "isPrivate": false }] }

POST /api/chat/channels
Body: { "name": "project-alpha", "userId": "..." }
Response: { "data": { "id": "...", "name": "project-alpha" } }
```

### Messages
```
GET /api/chat/channels/{channelId}/messages?limit=50
Response: { "data": [{ "id": "...", "username": "Alice", "content": "Hello", "type": "USER" }] }

POST /api/chat/messages
Body: { "channelId": "...", "userId": "...", "content": "Hello world" }
Response: { "data": { "id": "...", "content": "Hello world" } }
```

### Special Message Types

#### Create Channel
```
POST /api/chat/messages
Body: { "channelId": "...", "userId": "...", "content": "#project-alpha" }
Response: { "data": { "type": "channel_created", "channel": {...} } }
```

#### Call AI
```
POST /api/chat/messages
Body: { "channelId": "...", "userId": "...", "content": "@eking What is 2+2?" }
Response: { "data": { "id": "...", "content": "@eking What is 2+2?" } }
# AI response comes asynchronously as a new message
```

## Architecture

### Backend (Java)

**Models**:
- `User`: User information (id, nickname, online status)
- `Channel`: Channel information (id, name, private flag, members)
- `Message`: Message data (id, content, type, timestamp)

**Services**:
- `ChatService`: Singleton service managing users, channels, and messages in memory
  - Thread-safe using ConcurrentHashMap
  - Automatic private channel creation
  - Message history management

**Controllers**:
- `ChatController`: REST API endpoints for chat operations
  - Login
  - User management
  - Channel management
  - Message handling
  - AI integration

**AI Integration**:
- Uses existing `OllamaService` 
- Processes `@eking` mentions asynchronously
- Sends AI responses as new messages

### Frontend (Vue.js)

**Components**:
- Login screen
- Channels sidebar
- Chat area with message history
- Message input with hints
- Users sidebar

**Features**:
- Reactive data binding
- Polling for updates
- Auto-scroll on new messages
- Keyboard shortcuts (Enter to send)

## Usage Examples

### 1. Login
```javascript
// Enter nickname "Alice" and click "Join Chat"
POST /api/chat/login
{ "nickname": "Alice" }
```

### 2. Send Message
```javascript
// Type "Hello everyone!" and press Enter or click Send
POST /api/chat/messages
{ 
  "channelId": "general",
  "userId": "...",
  "content": "Hello everyone!"
}
```

### 3. Create Channel
```javascript
// Type "#project-alpha" and press Enter
POST /api/chat/messages
{ 
  "channelId": "general",
  "userId": "...",
  "content": "#project-alpha"
}
// New channel is created and automatically selected
```

### 4. Call AI Assistant
```javascript
// Type "@eking What is the capital of France?" and press Enter
POST /api/chat/messages
{ 
  "channelId": "general",
  "userId": "...",
  "content": "@eking What is the capital of France?"
}
// AI responds asynchronously with answer
```

## Technical Details

### State Management
- In-memory storage using `ConcurrentHashMap`
- Thread-safe operations
- Singleton service pattern
- No database required (suitable for demo/prototype)

### Real-time Updates
- HTTP polling every 2 seconds
- Could be upgraded to WebSockets for true real-time
- Efficient message retrieval with optional limit

### AI Processing
- Asynchronous processing in separate thread
- Non-blocking API response
- Error handling with user-friendly messages
- Requires Ollama service to be running

### Security Considerations
- No authentication (demo/prototype only)
- CORS enabled for development
- Input validation on backend
- SQL injection not applicable (in-memory storage)

## Future Enhancements

1. **WebSocket Support**: Replace polling with WebSocket for true real-time updates
2. **Persistence**: Add database storage for chat history
3. **Authentication**: Add proper user authentication
4. **File Sharing**: Support image and file uploads
5. **Typing Indicators**: Show when users are typing
6. **Read Receipts**: Show message read status
7. **Channel Permissions**: Add admin/moderator roles
8. **Direct Messages**: Add 1-on-1 private messaging
9. **Emoji Support**: Add emoji picker and reactions
10. **Search**: Add message and channel search

## Known Issues

1. **CDN Dependencies**: Vue.js and Axios are loaded from CDN. If CDN is blocked or unavailable, the application will not load. Consider bundling these libraries locally for production use.

2. **In-Memory Storage**: All data is stored in memory and will be lost on server restart. For production, implement database persistence.

3. **No Rate Limiting**: API endpoints have no rate limiting. Add rate limiting for production use.

4. **Single Server**: No horizontal scaling support. For production, implement session storage in Redis or similar.

## Testing

Unit tests are provided in `ChatServiceTest.java`:
```bash
mvn test -Dtest=ChatServiceTest
```

Manual API testing:
```bash
# See test scripts in /tmp/test-chat.sh and /tmp/test-ai.sh
```

## Troubleshooting

### Issue: Application won't start
**Solution**: Check that port 8080 is available and `application.properties` exists

### Issue: AI not responding
**Solution**: Ensure Ollama service is running at the configured URL (default: http://localhost:11434)

### Issue: Messages not appearing
**Solution**: Check browser console for errors, verify polling is working

### Issue: Vue.js not loading
**Solution**: Check browser can access CDN (https://cdn.jsdelivr.net), or bundle Vue.js locally
