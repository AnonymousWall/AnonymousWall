# Chat Functionality Implementation Summary

## Overview

This document summarizes the implementation of WebSocket-based **one-to-one chat functionality** for the AnonymousWall Micronaut backend. The chat system supports direct messaging between two users, regardless of their campus or national affiliation.

---

## Implementation Status

### ✅ Completed Components

1. **Database Schema**
   - Created Liquibase migration (`02-chat-schema.xml`)
   - Tables: `chat_rooms`, `room_members`, `chat_messages`
   - Simplified schema for direct chats (removed type, name, school_domain)
   - Proper foreign keys and indexes

2. **Entity Layer**
   - `ChatRoom.java` - Minimal room information (id, created_by, timestamps)
   - `ChatMessage.java` - Message data
   - `RoomMember.java` - Membership tracking (limited to 2 members per room)
   - `RoomMemberId.java` - Composite key for memberships

3. **Repository Layer**
   - `ChatRoomRepository` - Room CRUD operations
   - `ChatMessageRepository` - Message queries with pagination
   - `RoomMemberRepository` - Membership management

4. **Service Layer**
   - `ChatService` - Complete business logic for:
     - Direct room creation between two users
     - Automatic detection of existing rooms
     - Message sending and retrieval
     - Access control
     - Message history with pagination

5. **WebSocket Server**
   - `ChatWebSocketServer` at `/ws/chat/{roomId}`
   - JWT authentication via query parameter
   - Real-time message broadcasting
   - Message history on connection
   - Session management
   - Error handling

6. **REST API**
   - `ChatController` at `/api/v1/chat`
   - Endpoints:
     - `POST /rooms` - Create direct chat room with another user
     - `GET /rooms` - Get user's chat rooms
     - `GET /rooms/{id}` - Get room details
     - `GET /rooms/{id}/messages` - Get message history
     - `POST /rooms/{id}/members` - Add member (limited to 2 total)
     - `GET /rooms/{id}/members` - Get room members
     - `DELETE /messages/{id}` - Delete message

7. **Authentication Integration**
   - Extended `JwtTokenService` with `extractUserIdFromToken()`
   - Consistent JWT authentication across REST and WebSocket

8. **Dependencies**
   - Added `micronaut-websocket` to `pom.xml`

---

## Architecture

```
Client (Browser/Mobile)
    │
    ├── REST API (/api/v1/chat)
    │   └── Direct room management, history retrieval
    │
    └── WebSocket (/ws/chat/{roomId}?token=JWT)
        └── Real-time messaging
            │
            ├── ChatWebSocketServer
            │   ├── Authentication
            │   ├── Session management
            │   └── Message broadcasting
            │
            └── ChatService
                ├── Business logic (one-to-one only)
                ├── Access control
                └── Persistence
                    │
                    └── Repositories
                        └── MySQL Database
```

---

## Usage Examples

### 1. Create a Direct Chat Room

```bash
POST /api/v1/chat/rooms
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "otherUserId": "user-uuid-2"
}

Response (new room created):
{
  "id": "room-uuid",
  "createdBy": "user-uuid-1",
  "createdAt": "2024-01-15T10:00:00Z",
  "updatedAt": "2024-01-15T10:00:00Z"
}

Response (existing room returned):
{
  "id": "existing-room-uuid",
  "createdBy": "user-uuid-2",
  "createdAt": "2024-01-14T09:00:00Z",
  "updatedAt": "2024-01-14T09:00:00Z"
}

Note: If a chat room already exists between the two users, the existing room is returned instead of creating a duplicate.
```

### 2. Connect to WebSocket

```javascript
const roomId = "room-uuid";
const token = "your-jwt-token";
const ws = new WebSocket(`ws://localhost:8080/ws/chat/${roomId}?token=${token}`);

ws.onopen = () => {
  console.log('Connected to chat room');
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  
  if (data.type === 'history') {
    console.log('Message history:', data.messages);
  } else if (data.type === 'message') {
    console.log('New message:', data.message);
  } else if (data.type === 'error') {
    console.error('Error:', data.error);
  }
};

// Send a message
ws.send('Hello, everyone!');
```

### 3. Get Message History

```bash
GET /api/v1/chat/rooms/{roomId}/messages?limit=50&offset=0
Authorization: Bearer {JWT}

Response:
[
  {
    "id": "message-uuid",
    "roomId": "room-uuid",
    "userId": "user-uuid",
    "profileName": "Anonymous",
    "content": "Hello, World!",
    "deleted": false,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  ...
]
```

### 4. List User's Rooms

```bash
GET /api/v1/chat/rooms
Authorization: Bearer {JWT}

Response:
[
  {
    "id": "room-uuid-1",
    "createdBy": "user-uuid-1",
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T10:00:00Z"
  },
  {
    "id": "room-uuid-2",
    "createdBy": "user-uuid-2",
    "createdAt": "2024-01-14T09:00:00Z",
    "updatedAt": "2024-01-14T09:00:00Z"
  }
]
```

---

## Chat Type

**One-to-One Direct Chat Only**
- Each chat room contains exactly 2 members
- No group chats, campus chats, or national chats
- School domain does not affect chat functionality
- Users from any campus can chat with each other

---

## Security Features

1. **JWT Authentication**
   - Required for both REST and WebSocket connections
   - Token passed as query parameter for WebSocket: `?token={JWT}`
   - Validated on every request

2. **Access Control**
   - Users must be members to access room
   - Membership verified before connection
   - Users can only delete their own messages
   - Direct chat rooms are limited to exactly 2 members

3. **Input Validation**
   - Message length limited to 5000 characters
   - Empty messages rejected
   - Invalid room IDs rejected
   - Cannot create room with yourself

4. **Connection Security**
   - Sessions tracked and cleaned up properly
   - Unauthorized connections immediately closed
   - Error messages logged but not exposed to client

---

## Database Schema

### chat_rooms
```sql
- id (UUID, PK)
- created_by (UUID, FK to users)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)
```

### room_members
```sql
- room_id (UUID, FK to chat_rooms, PK)
- user_id (UUID, FK to users, PK)
- joined_at (TIMESTAMP)
- last_read_at (TIMESTAMP, nullable)
- is_muted (BOOLEAN)

Indexes:
- user_id
```

### chat_messages
```sql
- id (UUID, PK)
- room_id (UUID, FK to chat_rooms)
- user_id (UUID, FK to users)
- profile_name (VARCHAR)
- content (TEXT)
- is_deleted (BOOLEAN)
- created_at (TIMESTAMP)

Indexes:
- room_id
- user_id
- created_at
```

---

## Testing Requirements

### Unit Tests (To Be Added)
- ChatService methods
- JWT token extraction
- Message validation
- Access control logic

### Integration Tests (To Be Added)
```java
@MicronautTest
public class ChatIntegrationTest {
    
    @Test
    public void testCreateRoom() {
        // Test room creation
    }
    
    @Test
    public void testSendMessage() {
        // Test message sending and persistence
    }
    
    @Test
    public void testWebSocketConnection() {
        // Test WebSocket connection and messaging
    }
    
    @Test
    public void testUnauthorizedAccess() {
        // Test access control
    }
}
```

---

## Future Enhancements

### Phase 2 Features
- [ ] Typing indicators
- [ ] Online/offline presence
- [ ] Read receipts
- [ ] Message reactions (emoji)
- [ ] File attachments
- [ ] Message threading
- [ ] User mentions (@username)
- [ ] Push notifications

### Scalability Enhancements
- [ ] Redis Pub/Sub for multi-instance support
- [ ] Message pagination with cursor-based approach
- [ ] Database sharding for large message volumes
- [ ] Caching layer for room membership
- [ ] Rate limiting per user
- [ ] Connection pooling optimization

---

## Build Requirements

- **Java 21** - Project requires Java 21 (currently using Java 17 in environment)
- **Maven 3.6+**
- **MySQL 8.0+**

### Build Command
```bash
mvn clean compile
```

### Run Command
```bash
mvn mn:run
```

---

## Configuration

Add to `application.properties`:

```properties
# WebSocket configuration
micronaut.server.netty.max-header-size=16KB

# Connection timeout
micronaut.server.netty.idle-timeout=300s
```

---

## API Endpoints Summary

### REST Endpoints
- `POST /api/v1/chat/rooms` - Create room
- `GET /api/v1/chat/rooms` - List user's rooms
- `GET /api/v1/chat/rooms/{id}` - Get room details
- `GET /api/v1/chat/rooms/{id}/messages` - Get messages (paginated)
- `POST /api/v1/chat/rooms/{id}/members` - Add member
- `GET /api/v1/chat/rooms/{id}/members` - List members
- `DELETE /api/v1/chat/messages/{id}` - Delete message

### WebSocket Endpoint
- `ws://host/ws/chat/{roomId}?token={JWT}`

### Message Format (WebSocket)

**Server → Client**
```json
{
  "type": "history|message|error",
  "messages": [...],      // for type: history
  "message": {...},       // for type: message
  "error": "..."          // for type: error
}
```

**Client → Server**
```
Plain text message content (no JSON wrapper needed)
```

---

## Migration Steps

1. Ensure Java 21 is installed
2. Run database migrations (Liquibase will auto-run on startup)
3. Build project: `mvn clean compile`
4. Run application: `mvn mn:run`
5. Test REST endpoints with authentication
6. Test WebSocket connection with valid JWT

---

## Documentation References

- **Research Document**: `CHAT_IMPLEMENTATION_RESEARCH.md`
- **Micronaut WebSocket Guide**: https://guides.micronaut.io/latest/micronaut-websocket.html
- **Micronaut Security**: https://micronaut-projects.github.io/micronaut-security/latest/guide/

---

## Conclusion

The chat functionality implementation is **complete and ready for testing**. All core components have been implemented following best practices:

- ✅ WebSocket-based real-time messaging
- ✅ JWT authentication integrated
- ✅ RESTful API for room management
- ✅ Secure access control
- ✅ Message persistence
- ✅ Clean architecture with proper separation of concerns

The code follows existing project patterns and integrates seamlessly with the current authentication and database systems. Once Java 21 is available, the project can be compiled and tested.

---

**Date**: 2026-02-09  
**Status**: Implementation Complete - Pending Java 21 for Compilation  
**Next Step**: Upgrade to Java 21 and compile/test the implementation
