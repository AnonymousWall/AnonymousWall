# WebSocket Authentication Guide

## Overview

The WebSocket endpoint at `/ws/chat` requires JWT authentication. The endpoint is secured with `@Secured(SecurityRule.IS_AUTHENTICATED)`, which means only authenticated users can connect.

Supports two authentication methods (in order of preference):

1. **Sec-WebSocket-Protocol header** (recommended - more secure)
2. **Query parameters** (fallback for compatibility)

## Authentication Methods

### Method 1: Sec-WebSocket-Protocol Header (Recommended)

This is the **most secure** method as tokens don't appear in URLs or server logs.

#### Connection Format

```javascript
// Plain format (recommended)
const socket = new WebSocket('ws://localhost:8080/ws/chat', [jwtToken]);

// Bearer prefix format (also supported)
const socket = new WebSocket('ws://localhost:8080/ws/chat', ['Bearer.' + jwtToken]);
```

#### Why This is Better
- ✅ Tokens don't appear in URLs
- ✅ Not logged by proxies or load balancers
- ✅ Not stored in browser history
- ✅ Industry best practice for WebSocket security

### Method 2: Query Parameters (Fallback)

Use this method if you need backward compatibility or can't use the header method.

```
ws://host:port/ws/chat?token=YOUR_JWT_TOKEN
```

or

```
ws://host:port/ws/chat?access_token=YOUR_JWT_TOKEN
```

## Examples

### Using wscat (Command Line)

#### Sec-WebSocket-Protocol Header (Recommended)
```bash
# Plain format
wscat -c ws://localhost:8080/ws/chat --subprotocol 'eyJhbGciOiJIUzI1NiJ9...'

# Bearer format
wscat -c ws://localhost:8080/ws/chat --subprotocol 'Bearer.eyJhbGciOiJIUzI1NiJ9...'
```

#### Query Parameter (Fallback)
```bash
# Make sure to quote the URL to prevent shell interpretation of special characters
wscat -c 'ws://localhost:8080/ws/chat?token=eyJhbGciOiJIUzI1NiJ9...'
```

### Using JavaScript (Browser)

#### Sec-WebSocket-Protocol Header (Recommended)
```javascript
// Get your JWT token from login response
const token = 'eyJhbGciOiJIUzI1NiJ9...';

// Connect using Sec-WebSocket-Protocol header (most secure)
const socket = new WebSocket('ws://localhost:8080/ws/chat', [token]);

socket.onopen = (event) => {
    console.log('WebSocket connected:', event);
};

socket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('Received message:', message);
};

socket.onerror = (error) => {
    console.error('WebSocket error:', error);
};

socket.onclose = (event) => {
    console.log('WebSocket closed:', event);
};
```

#### Query Parameter (Fallback)
```javascript
// Get your JWT token from login response
const token = 'eyJhbGciOiJIUzI1NiJ9...';

// Connect to WebSocket with token in query parameter
const socket = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

socket.onopen = (event) => {
    console.log('WebSocket connected:', event);
};

socket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('Received message:', message);
};

socket.onerror = (error) => {
    console.error('WebSocket error:', error);
};

socket.onclose = (event) => {
    console.log('WebSocket closed:', event);
};
```

### Using Java WebSocket Client

```java
import io.micronaut.websocket.WebSocketClient;
import io.micronaut.http.client.annotation.Client;

@Client("ws://localhost:8080")
interface ChatWebSocketClient {
    @ClientWebSocket("/ws/chat?token=${token}")
    Publisher<String> connect(@PathVariable String token);
}
```

### Using Swift/iOS (URLSessionWebSocketTask)

#### Sec-WebSocket-Protocol Header (Recommended)

⚠️ **Important**: In Swift, you cannot use `setValue(_:forHTTPHeaderField:)` for the `Sec-WebSocket-Protocol` header with URLSessionWebSocketTask. Instead, you must pass protocols in the URLSessionWebSocketTask initializer.

**❌ INCORRECT - Don't do this:**
```swift
// This will NOT work - setValue cannot be used for Sec-WebSocket-Protocol
var request = URLRequest(url: url)
request.setValue(token, forHTTPHeaderField: "Sec-WebSocket-Protocol")
```

**✅ CORRECT - Use URLSessionConfiguration:**

```swift
import Foundation
import Combine

class WebSocketManager {
    private var webSocketTask: URLSessionWebSocketTask?
    private var session: URLSession?
    
    func connect(token: String) {
        // Build WebSocket URL
        let wsURLString = "wss://your-domain.com/ws/chat"
        guard let url = URL(string: wsURLString) else { return }
        
        // Create URLRequest
        var request = URLRequest(url: url)
        
        // Create URLSessionConfiguration
        let config = URLSessionConfiguration.default
        
        // Set additional headers if needed (NOT Sec-WebSocket-Protocol)
        request.addValue("iOS", forHTTPHeaderField: "User-Agent")
        
        // Create URLSession with protocols in the WebSocketTask
        session = URLSession(configuration: config)
        
        // Create WebSocket task with protocols array
        // Plain format (recommended)
        webSocketTask = session?.webSocketTask(with: request, protocols: [token])
        
        // Alternative: Bearer format
        // webSocketTask = session?.webSocketTask(with: request, protocols: ["Bearer.\(token)"])
        
        // Start connection
        webSocketTask?.resume()
        
        // Start receiving messages
        receiveMessage()
    }
    
    private func receiveMessage() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    print("Received text: \(text)")
                    // Parse JSON and handle message
                    self?.handleMessage(text)
                case .data(let data):
                    print("Received data: \(data)")
                @unknown default:
                    break
                }
                // Continue receiving
                self?.receiveMessage()
                
            case .failure(let error):
                print("WebSocket error: \(error)")
            }
        }
    }
    
    func sendMessage(_ text: String) {
        let message = URLSessionWebSocketTask.Message.string(text)
        webSocketTask?.send(message) { error in
            if let error = error {
                print("Send error: \(error)")
            }
        }
    }
    
    func disconnect() {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
    }
    
    private func handleMessage(_ text: String) {
        // Parse and handle your message
        guard let data = text.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = json["type"] as? String else {
            return
        }
        
        switch type {
        case "connected":
            print("Successfully connected")
        case "message":
            print("Received chat message")
        default:
            print("Unknown message type: \(type)")
        }
    }
}
```

#### Query Parameter (Fallback)

If you need to use query parameters instead (less secure):

```swift
func connectWithQueryParam(token: String) {
    let wsURLString = "wss://your-domain.com/ws/chat?token=\(token)"
    guard let url = URL(string: wsURLString) else { return }
    
    let request = URLRequest(url: url)
    let config = URLSessionConfiguration.default
    session = URLSession(configuration: config)
    
    // No protocols needed for query parameter method
    webSocketTask = session?.webSocketTask(with: request)
    webSocketTask?.resume()
    
    receiveMessage()
}
```

#### Complete Example with Error Handling

```swift
import Foundation
import Combine

enum WebSocketError: Error {
    case invalidURL
    case connectionFailed
    case unauthorized
}

class ChatWebSocketService {
    private var webSocketTask: URLSessionWebSocketTask?
    private var session: URLSession?
    private let connectionState = PassthroughSubject<ConnectionState, Never>()
    
    enum ConnectionState {
        case disconnected
        case connecting
        case connected
        case failed(Error)
    }
    
    func connect(token: String, useSecureProtocol: Bool = true) {
        connectionState.send(.connecting)
        
        guard let url = URL(string: "wss://your-domain.com/ws/chat") else {
            connectionState.send(.failed(WebSocketError.invalidURL))
            return
        }
        
        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        
        let config = URLSessionConfiguration.default
        session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
        
        if useSecureProtocol {
            // Recommended: Use Sec-WebSocket-Protocol
            webSocketTask = session?.webSocketTask(with: request, protocols: [token])
        } else {
            // Fallback: Use query parameter
            guard let urlWithToken = URL(string: "wss://your-domain.com/ws/chat?token=\(token)") else {
                connectionState.send(.failed(WebSocketError.invalidURL))
                return
            }
            request = URLRequest(url: urlWithToken)
            webSocketTask = session?.webSocketTask(with: request)
        }
        
        webSocketTask?.resume()
        receiveMessage()
    }
    
    private func receiveMessage() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .success(let message):
                self?.connectionState.send(.connected)
                self?.handleMessage(message)
                self?.receiveMessage()
            case .failure(let error):
                self?.connectionState.send(.failed(error))
            }
        }
    }
    
    private func handleMessage(_ message: URLSessionWebSocketTask.Message) {
        switch message {
        case .string(let text):
            print("📨 Received: \(text)")
        case .data(let data):
            print("📦 Received data: \(data)")
        @unknown default:
            break
        }
    }
    
    func send(message: String, completion: @escaping (Error?) -> Void) {
        let wsMessage = URLSessionWebSocketTask.Message.string(message)
        webSocketTask?.send(wsMessage, completionHandler: completion)
    }
    
    func disconnect() {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        connectionState.send(.disconnected)
    }
}

// MARK: - URLSessionWebSocketDelegate
extension ChatWebSocketService: URLSessionWebSocketDelegate {
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        print("✅ WebSocket connected with protocol: \(protocol ?? "none")")
        connectionState.send(.connected)
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        print("❌ WebSocket closed with code: \(closeCode)")
        connectionState.send(.disconnected)
    }
}
```

#### Usage Example

```swift
let webSocketService = ChatWebSocketService()

// Connect with Sec-WebSocket-Protocol (recommended)
webSocketService.connect(token: jwtToken, useSecureProtocol: true)

// Or use query parameter (fallback)
webSocketService.connect(token: jwtToken, useSecureProtocol: false)

// Send a message
let messageJSON = """
{
    "type": "message",
    "receiverId": "uuid-here",
    "content": "Hello from iOS!"
}
"""
webSocketService.send(message: messageJSON) { error in
    if let error = error {
        print("Failed to send: \(error)")
    }
}
```

### Using Swift/iOS (Third-Party Libraries)

If using Starscream or similar libraries:

```swift
import Starscream

// With Sec-WebSocket-Protocol
var request = URLRequest(url: URL(string: "wss://your-domain.com/ws/chat")!)
request.timeoutInterval = 10

let socket = WebSocket(request: request)
socket.connect(headers: [:], protocols: [token])  // Pass token as protocol

// With query parameter (fallback)
let socketWithQuery = WebSocket(url: URL(string: "wss://your-domain.com/ws/chat?token=\(token)")!)
socketWithQuery.connect()
```

## Authentication Priority

When multiple authentication methods are provided, the server checks in this order:

1. **Sec-WebSocket-Protocol header** (checked first)
2. **token query parameter** (checked second)
3. **access_token query parameter** (checked last)

## Authentication Flow

1. **Obtain JWT Token**: First, authenticate via the REST API (e.g., `/api/auth/login`) to get a JWT token
2. **Connect with Token**: Use the token via Sec-WebSocket-Protocol header (recommended) or query parameter
3. **Connection Success**: If the token is valid, you'll receive a connection confirmation message
4. **Connection Failure**: Invalid or expired tokens will result in a 401 or 403 error

## Message Format

### Connection Confirmation (Server → Client)

```json
{
  "type": "connected",
  "userId": "your-user-id-uuid",
  "timestamp": 1234567890
}
```

### Unread Count Notification (Server → Client)

```json
{
  "type": "unread_count",
  "count": 5
}
```

### Send Message (Client → Server)

```json
{
  "type": "message",
  "receiverId": "receiver-user-id-uuid",
  "content": "Hello, World!"
}
```

### Receive Message (Server → Client)

```json
{
  "type": "message",
  "message": {
    "id": "message-id-uuid",
    "senderId": "sender-user-id-uuid",
    "receiverId": "receiver-user-id-uuid",
    "content": "Hello, World!",
    "readStatus": false,
    "createdAt": "2026-02-15T09:00:00Z"
  }
}
```

### Typing Indicator (Client → Server)

```json
{
  "type": "typing",
  "receiverId": "receiver-user-id-uuid"
}
```

### Mark as Read (Client → Server)

```json
{
  "type": "mark_read",
  "messageId": "message-id-uuid"
}
```

### Error Response (Server → Client)

```json
{
  "type": "error",
  "error": "Error message description"
}
```

## Security Notes

1. **Use WSS in Production**: Always use `wss://` (WebSocket Secure) in production to encrypt the token during transmission
2. **Token Expiration**: JWT tokens expire after a configured duration. Implement reconnection logic to handle expired tokens
3. **Token in URL**: While query parameters are standard for WebSocket authentication, be aware they may appear in server logs
4. **CORS**: Ensure proper CORS configuration if connecting from a web browser

## Troubleshooting

### 401 Unauthorized
- Token is missing or invalid
- Token has expired
- Token signature doesn't match

### 403 Forbidden
- User account is blocked
- User doesn't have required permissions

### Connection Refused
- Server is not running
- Wrong host or port
- Firewall blocking the connection

### No Logs in ChatWebSocketHandler
- **This was the original issue**: The WebSocket connection wasn't reaching the handler due to missing query parameter token support
- **Solution**: The `WebSocketTokenReader` class now extracts tokens from query parameters, enabling proper authentication
