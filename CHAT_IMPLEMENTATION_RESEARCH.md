# Chat Functionality Implementation Research

## Executive Summary

This document provides a comprehensive research analysis and recommendation for implementing real-time chat functionality in the AnonymousWall Micronaut backend. After analyzing the current architecture and evaluating multiple approaches, **WebSockets** is recommended as the best solution for bidirectional, real-time messaging.

---

## Current Architecture Analysis

### Technology Stack
- **Framework**: Micronaut 4.10.7 (Java 21)
- **Database**: MySQL with Liquibase migrations
- **Authentication**: JWT-based security
- **API Design**: OpenAPI/Swagger specification-driven
- **Data Access**: Micronaut Data JDBC

### Existing Features
- User registration with email verification
- Anonymous posting to campus/national walls
- Comments, likes, and reporting
- School domain-based segregation
- JWT authentication with bearer tokens

### Key Observations
1. Already uses JWT authentication - can be reused for WebSocket connections
2. Liquibase for database migrations - easy to extend schema
3. Anonymous user model - chat can be anonymous or identified
4. School domain segregation - useful for school-specific chat rooms

---

## Technology Comparison

### Option 1: WebSockets ✅ **RECOMMENDED**

**Description**: Full-duplex, bidirectional communication over persistent TCP connection.

**Pros**:
- ✅ True bidirectional real-time communication
- ✅ Low latency and minimal overhead after initial handshake
- ✅ Excellent Micronaut support via `micronaut-websocket` module
- ✅ Perfect for interactive chat features (typing indicators, instant delivery, presence)
- ✅ Scalable with proper session management
- ✅ Native browser support (no additional libraries needed)
- ✅ Can send both text and binary data

**Cons**:
- ⚠️ Requires persistent connection (more server resources)
- ⚠️ Some proxies/firewalls may block WebSocket connections
- ⚠️ Requires stateful session management

**Use Cases**:
- One-on-one direct messaging
- Group chat rooms
- Real-time notifications
- Typing indicators
- Online presence/status

**Implementation Complexity**: Medium

---

### Option 2: Server-Sent Events (SSE)

**Description**: Unidirectional communication where server pushes updates to clients over HTTP.

**Pros**:
- ✅ Simple implementation
- ✅ Works over standard HTTP (firewall-friendly)
- ✅ Automatic reconnection support
- ✅ Good Micronaut support

**Cons**:
- ❌ One-way only (server → client)
- ❌ Requires separate HTTP POST for client → server
- ❌ Less efficient for interactive chat
- ❌ Limited browser connection limits (typically 6 per domain)

**Use Cases**:
- Broadcast notifications
- Live feed updates
- Dashboard streaming

**Implementation Complexity**: Low

---

### Option 3: HTTP Polling/Long Polling

**Description**: Client repeatedly requests server for new messages.

**Pros**:
- ✅ Simple and universally supported
- ✅ No special infrastructure needed

**Cons**:
- ❌ High latency
- ❌ Inefficient (many empty requests)
- ❌ High server load
- ❌ Not suitable for real-time chat

**Use Cases**:
- Legacy systems only
- Fallback mechanism

**Implementation Complexity**: Very Low

---

## Recommended Solution: WebSockets

### Why WebSockets?

1. **Real-Time Requirements**: Chat needs instant message delivery in both directions
2. **Micronaut Support**: First-class WebSocket support with annotations
3. **User Experience**: Enables rich features like typing indicators, presence, and instant notifications
4. **Scalability**: Efficient after initial handshake
5. **Industry Standard**: Widely used for chat applications (WhatsApp Web, Slack, Discord, etc.)

---

## Proposed Architecture

### High-Level Design

```
┌─────────────┐
│   Client    │
│  (Browser/  │
│   Mobile)   │
└──────┬──────┘
       │
       │ WebSocket Connection
       │ wss://api.example.com/ws/chat?token=JWT
       │
┌──────▼──────────────────────────────────┐
│         Micronaut Backend               │
│  ┌────────────────────────────────┐    │
│  │  ChatWebSocketServer           │    │
│  │  - @ServerWebSocket("/ws/chat")│    │
│  │  - Authentication Filter       │    │
│  │  - Session Management          │    │
│  └────────┬───────────────────────┘    │
│           │                              │
│  ┌────────▼───────────────────────┐    │
│  │  ChatService                    │    │
│  │  - Send/receive messages        │    │
│  │  - Room management              │    │
│  │  - Message persistence          │    │
│  └────────┬───────────────────────┘    │
│           │                              │
│  ┌────────▼───────────────────────┐    │
│  │  ChatRepository (Micronaut Data)│    │
│  │  - Message CRUD                 │    │
│  │  - Room CRUD                    │    │
│  └────────┬───────────────────────┘    │
│           │                              │
└───────────┼──────────────────────────────┘
            │
    ┌───────▼────────┐
    │  MySQL Database │
    │  - chat_rooms   │
    │  - chat_messages│
    │  - room_members │
    └─────────────────┘
```

---

## Database Schema Design

### Table: `chat_rooms`
Stores chat room information (group chats, direct messages, or school-wide chats).

```sql
CREATE TABLE chat_rooms (
    id CHAR(36) PRIMARY KEY,                    -- UUID
    name VARCHAR(255),                           -- Room name (null for direct messages)
    type VARCHAR(20) NOT NULL,                   -- 'DIRECT', 'GROUP', 'CAMPUS', 'NATIONAL'
    school_domain VARCHAR(255),                  -- For CAMPUS type rooms
    created_by CHAR(36) NOT NULL,                -- Foreign key to users.id
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_chat_rooms_type ON chat_rooms(type);
CREATE INDEX idx_chat_rooms_school_domain ON chat_rooms(school_domain);
```

### Table: `room_members`
Tracks which users belong to which chat rooms.

```sql
CREATE TABLE room_members (
    room_id CHAR(36) NOT NULL,                   -- Foreign key to chat_rooms.id
    user_id CHAR(36) NOT NULL,                   -- Foreign key to users.id
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_read_at TIMESTAMP NULL,                 -- For unread message tracking
    is_muted BOOLEAN DEFAULT FALSE,              -- Allow users to mute rooms
    PRIMARY KEY (room_id, user_id),
    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_room_members_user_id ON room_members(user_id);
```

### Table: `chat_messages`
Stores all chat messages.

```sql
CREATE TABLE chat_messages (
    id CHAR(36) PRIMARY KEY,                     -- UUID
    room_id CHAR(36) NOT NULL,                   -- Foreign key to chat_rooms.id
    user_id CHAR(36) NOT NULL,                   -- Foreign key to users.id (sender)
    profile_name VARCHAR(255) DEFAULT 'Anonymous', -- Sender's display name at time of sending
    content TEXT NOT NULL,                       -- Message content
    is_deleted BOOLEAN DEFAULT FALSE,            -- Soft delete
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_room_id ON chat_messages(room_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
```

### Optional: `message_reactions` (Future Enhancement)
For emoji reactions to messages.

```sql
CREATE TABLE message_reactions (
    id CHAR(36) PRIMARY KEY,
    message_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_message_emoji (message_id, user_id, emoji)
);
```

---

## Implementation Components

### 1. Maven Dependencies

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.micronaut.websocket</groupId>
    <artifactId>micronaut-websocket</artifactId>
    <scope>compile</scope>
</dependency>
```

### 2. Entities

**ChatRoom.java**
```java
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    private String id;
    private String name;
    @Enumerated(EnumType.STRING)
    private RoomType type;
    private String schoolDomain;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

enum RoomType {
    DIRECT,    // 1-on-1 chat
    GROUP,     // Private group chat
    CAMPUS,    // School-wide chat
    NATIONAL   // Cross-campus chat
}
```

**ChatMessage.java**
```java
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    private String id;
    private String roomId;
    private String userId;
    private String profileName;
    private String content;
    private boolean isDeleted;
    private LocalDateTime createdAt;
}
```

**RoomMember.java**
```java
@Entity
@Table(name = "room_members")
public class RoomMember {
    @EmbeddedId
    private RoomMemberId id;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private boolean isMuted;
}

@Embeddable
class RoomMemberId {
    private String roomId;
    private String userId;
}
```

### 3. Repositories

```java
@Repository
public interface ChatRoomRepository extends CrudRepository<ChatRoom, String> {
    List<ChatRoom> findByTypeAndSchoolDomain(RoomType type, String schoolDomain);
}

@Repository
public interface ChatMessageRepository extends CrudRepository<ChatMessage, String> {
    @Query("SELECT * FROM chat_messages WHERE room_id = :roomId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, int limit, int offset);
}

@Repository
public interface RoomMemberRepository extends CrudRepository<RoomMember, RoomMemberId> {
    List<RoomMember> findByUserId(String userId);
    List<RoomMember> findByRoomId(String roomId);
}
```

### 4. WebSocket Server

```java
@ServerWebSocket("/ws/chat")
@Singleton
public class ChatWebSocketServer {
    
    private final ChatService chatService;
    private final JwtTokenService jwtTokenService;
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionUsers = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(String roomId, WebSocketSession session) {
        // 1. Extract and validate JWT from query parameter
        String token = session.getRequestParameter("token").orElse(null);
        if (!isValidToken(token)) {
            session.close(CloseReason.UNAUTHORIZED);
            return;
        }
        
        // 2. Get user from token
        String userId = getUserIdFromToken(token);
        
        // 3. Verify user has access to room
        if (!chatService.canAccessRoom(userId, roomId)) {
            session.close(CloseReason.FORBIDDEN);
            return;
        }
        
        // 4. Add session to room
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionUsers.put(session, userId);
        
        // 5. Send recent message history
        List<ChatMessage> history = chatService.getRecentMessages(roomId, 50);
        session.sendSync(history);
    }
    
    @OnMessage
    public void onMessage(String roomId, String message, WebSocketSession session) {
        String userId = sessionUsers.get(session);
        
        // Save message to database
        ChatMessage chatMessage = chatService.sendMessage(roomId, userId, message);
        
        // Broadcast to all room members
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                s.sendAsync(chatMessage);
            }
        }
    }
    
    @OnClose
    public void onClose(String roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
        }
        sessionUsers.remove(session);
    }
    
    @OnError
    public void onError(String roomId, WebSocketSession session, Throwable error) {
        onClose(roomId, session);
    }
}
```

### 5. Service Layer

```java
@Singleton
public class ChatService {
    
    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    
    public ChatMessage sendMessage(String roomId, String userId, String content) {
        // Verify room membership
        if (!isMember(roomId, userId)) {
            throw new ForbiddenException("User is not a member of this room");
        }
        
        // Create and save message
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        
        return messageRepository.save(message);
    }
    
    public ChatRoom createRoom(String createdBy, RoomType type, String name, String schoolDomain) {
        ChatRoom room = new ChatRoom();
        room.setId(UUID.randomUUID().toString());
        room.setName(name);
        room.setType(type);
        room.setSchoolDomain(schoolDomain);
        room.setCreatedBy(createdBy);
        
        ChatRoom saved = roomRepository.save(room);
        
        // Add creator as member
        addMember(saved.getId(), createdBy);
        
        return saved;
    }
    
    public void addMember(String roomId, String userId) {
        RoomMember member = new RoomMember();
        member.setId(new RoomMemberId(roomId, userId));
        member.setJoinedAt(LocalDateTime.now());
        memberRepository.save(member);
    }
    
    public List<ChatMessage> getRecentMessages(String roomId, int limit) {
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, limit, 0);
    }
    
    public boolean canAccessRoom(String userId, String roomId) {
        return isMember(roomId, userId);
    }
    
    private boolean isMember(String roomId, String userId) {
        return memberRepository.existsById(new RoomMemberId(roomId, userId));
    }
}
```

### 6. REST Endpoints (for room management)

```java
@Controller("/api/v1/chat")
@Secured("isAuthenticated()")
public class ChatController {
    
    private final ChatService chatService;
    
    @Post("/rooms")
    public ChatRoom createRoom(@Body CreateRoomRequest request, Authentication auth) {
        String userId = auth.getName();
        return chatService.createRoom(userId, request.getType(), request.getName(), request.getSchoolDomain());
    }
    
    @Get("/rooms")
    public List<ChatRoom> getUserRooms(Authentication auth) {
        String userId = auth.getName();
        return chatService.getUserRooms(userId);
    }
    
    @Post("/rooms/{roomId}/members")
    public void addMember(@PathVariable String roomId, @Body AddMemberRequest request, Authentication auth) {
        chatService.addMember(roomId, request.getUserId());
    }
    
    @Get("/rooms/{roomId}/messages")
    public List<ChatMessage> getMessages(
        @PathVariable String roomId,
        @QueryValue(defaultValue = "50") int limit,
        @QueryValue(defaultValue = "0") int offset,
        Authentication auth
    ) {
        String userId = auth.getName();
        if (!chatService.canAccessRoom(userId, roomId)) {
            throw new ForbiddenException();
        }
        return chatService.getRecentMessages(roomId, limit, offset);
    }
}
```

---

## Security Considerations

### 1. Authentication
- **JWT Validation**: Validate JWT tokens on WebSocket connection
- **Token Expiry**: Handle token expiration gracefully
- **Query Parameter**: Pass token as query param: `ws://host/ws/chat/{roomId}?token=JWT`

### 2. Authorization
- **Room Access**: Verify user is member before allowing connection
- **Message Permissions**: Check permissions before saving messages
- **School Domain**: Enforce school domain restrictions for CAMPUS rooms

### 3. Data Validation
- **Message Length**: Limit message size (e.g., 5000 characters)
- **Rate Limiting**: Implement per-user rate limits to prevent spam
- **Content Filtering**: Optional profanity/spam filtering

### 4. Connection Security
- **Use WSS**: Always use `wss://` (WebSocket Secure) in production
- **Session Management**: Clean up stale sessions
- **Connection Limits**: Limit concurrent connections per user

---

## Scalability Considerations

### For Small to Medium Scale (< 10,000 concurrent users)
- Single instance with in-memory session management (as designed above)
- MySQL database for message persistence
- Simple broadcast pattern

### For Large Scale (> 10,000 concurrent users)
Consider:
1. **Message Broker**: Redis Pub/Sub or RabbitMQ for multi-instance message distribution
2. **Session Storage**: Redis for distributed session management
3. **Load Balancer**: Sticky sessions or consistent hashing
4. **Database Sharding**: Partition messages by room or time
5. **Caching**: Cache room membership and recent messages

Example with Redis:
```java
@Singleton
public class ChatWebSocketServer {
    
    @Inject
    RedisPublisher publisher;
    
    @Inject
    RedisSubscriber subscriber;
    
    @OnMessage
    public void onMessage(String roomId, String message, WebSocketSession session) {
        // Save to database
        ChatMessage chatMessage = chatService.sendMessage(roomId, userId, message);
        
        // Publish to Redis channel (for multi-instance)
        publisher.publish("chat:" + roomId, chatMessage);
    }
    
    @PostConstruct
    public void subscribeToRedis() {
        // Subscribe to room updates from other instances
        subscriber.subscribe("chat:*", (channel, message) -> {
            String roomId = channel.substring(5);
            broadcastToLocalSessions(roomId, message);
        });
    }
}
```

---

## Testing Strategy

### Unit Tests
- Test message validation
- Test room access control
- Test JWT validation logic

### Integration Tests
```java
@MicronautTest
public class ChatWebSocketTest {
    
    @Inject
    @Client("/")
    WebSocketClient client;
    
    @Test
    public void testSendAndReceiveMessage() {
        // Connect to WebSocket
        // Send message
        // Verify message received
        // Verify message persisted in database
    }
    
    @Test
    public void testUnauthorizedAccess() {
        // Attempt connection without valid JWT
        // Verify connection rejected
    }
}
```

### Load Tests
- Use tools like JMeter or K6 to simulate concurrent users
- Test message throughput
- Monitor memory usage and connection handling

---

## Client Implementation Example

### JavaScript/TypeScript
```javascript
class ChatClient {
    constructor(roomId, jwtToken) {
        this.roomId = roomId;
        this.token = jwtToken;
        this.ws = null;
    }
    
    connect() {
        this.ws = new WebSocket(
            `wss://api.example.com/ws/chat/${this.roomId}?token=${this.token}`
        );
        
        this.ws.onopen = () => {
            console.log('Connected to chat');
        };
        
        this.ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.displayMessage(message);
        };
        
        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
        
        this.ws.onclose = () => {
            console.log('Disconnected from chat');
            // Implement reconnection logic
        };
    }
    
    sendMessage(content) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify({ content }));
        }
    }
    
    disconnect() {
        if (this.ws) {
            this.ws.close();
        }
    }
}

// Usage
const chat = new ChatClient('room-uuid', 'jwt-token');
chat.connect();
chat.sendMessage('Hello, World!');
```

---

## OpenAPI Specification Updates

Add to `src/main/resources/api.yml`:

```yaml
tags:
  - name: chat
    description: Real-time chat functionality

paths:
  /chat/rooms:
    post:
      tags: [chat]
      summary: Create a new chat room
      operationId: createChatRoom
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateRoomRequest"
      responses:
        "201":
          description: Room created successfully
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ChatRoom"
    
    get:
      tags: [chat]
      summary: Get user's chat rooms
      operationId: getUserRooms
      security:
        - bearerAuth: []
      responses:
        "200":
          description: List of chat rooms
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/ChatRoom"

  /chat/rooms/{roomId}/messages:
    get:
      tags: [chat]
      summary: Get chat messages for a room
      operationId: getRoomMessages
      security:
        - bearerAuth: []
      parameters:
        - name: roomId
          in: path
          required: true
          schema:
            type: string
        - name: limit
          in: query
          schema:
            type: integer
            default: 50
        - name: offset
          in: query
          schema:
            type: integer
            default: 0
      responses:
        "200":
          description: List of messages
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/ChatMessage"

components:
  schemas:
    ChatRoom:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        type:
          type: string
          enum: [DIRECT, GROUP, CAMPUS, NATIONAL]
        schoolDomain:
          type: string
        createdBy:
          type: string
        createdAt:
          type: string
          format: date-time
    
    ChatMessage:
      type: object
      properties:
        id:
          type: string
        roomId:
          type: string
        userId:
          type: string
        profileName:
          type: string
        content:
          type: string
        isDeleted:
          type: boolean
        createdAt:
          type: string
          format: date-time
    
    CreateRoomRequest:
      type: object
      required:
        - type
      properties:
        name:
          type: string
        type:
          type: string
          enum: [DIRECT, GROUP, CAMPUS, NATIONAL]
        schoolDomain:
          type: string
        memberIds:
          type: array
          items:
            type: string
```

### WebSocket Connection
```
WebSocket Endpoint: ws://localhost:8080/ws/chat/{roomId}?token={JWT}

Message Format (Client → Server):
{
  "content": "Hello, World!"
}

Message Format (Server → Client):
{
  "id": "message-uuid",
  "roomId": "room-uuid",
  "userId": "user-uuid",
  "profileName": "Anonymous",
  "content": "Hello, World!",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1)
- [ ] Add WebSocket dependency
- [ ] Create database migration
- [ ] Implement entities and repositories
- [ ] Basic WebSocket server (echo test)

### Phase 2: Core Features (Week 2)
- [ ] Implement ChatService
- [ ] Add JWT authentication to WebSocket
- [ ] Message persistence
- [ ] REST endpoints for room management

### Phase 3: Features & Testing (Week 3)
- [ ] Room types (direct, group, campus)
- [ ] Message history loading
- [ ] Unit and integration tests
- [ ] API documentation

### Phase 4: Enhancement (Week 4)
- [ ] Typing indicators
- [ ] Online presence
- [ ] Message reactions
- [ ] Read receipts
- [ ] Load testing and optimization

---

## Alternative Considerations

### If WebSockets Don't Work

**Fallback to SSE + HTTP POST**:
- Use SSE for server → client (message notifications)
- Use HTTP POST for client → server (sending messages)
- Simpler but less real-time

**Hybrid Approach**:
- Primary: WebSocket
- Fallback: SSE + HTTP POST
- Detect support and choose automatically

---

## Monitoring & Observability

### Metrics to Track
- Active WebSocket connections
- Messages per second
- Message delivery latency
- Connection errors
- Room participation statistics

### Logging
```java
@Slf4j
@ServerWebSocket("/ws/chat")
public class ChatWebSocketServer {
    
    @OnOpen
    public void onOpen(String roomId, WebSocketSession session) {
        log.info("User connected to room: {}, session: {}", roomId, session.getId());
    }
    
    @OnMessage
    public void onMessage(String roomId, String message, WebSocketSession session) {
        log.debug("Message received in room {}: {}", roomId, message);
    }
}
```

---

## Conclusion

**WebSockets** is the clear choice for implementing chat functionality in the AnonymousWall Micronaut backend. It provides:

✅ Real-time bidirectional communication  
✅ Excellent Micronaut integration  
✅ Low latency and overhead  
✅ Rich feature support  
✅ Industry-standard solution  

The proposed architecture integrates seamlessly with the existing JWT authentication, database schema, and API design patterns. The phased implementation approach allows for incremental development and testing while maintaining code quality and system stability.

### Next Steps
1. Review and approve this research document
2. Begin Phase 1 implementation
3. Set up development environment for WebSocket testing
4. Create initial database migration
5. Implement basic WebSocket echo server for validation

---

## References

- [Micronaut WebSocket Official Guide](https://guides.micronaut.io/latest/micronaut-websocket-maven-java.html)
- [Micronaut Security JWT](https://micronaut-projects.github.io/micronaut-security/latest/guide/)
- [WebSocket RFC 6455](https://datatracker.ietf.org/doc/html/rfc6455)
- [Chat Application Database Design Best Practices](https://github.com/sudhanshutiwari264/Chat-Application-Database)

---

**Document Version**: 1.0  
**Date**: 2026-02-09  
**Author**: Copilot Research Agent  
**Status**: Ready for Review
