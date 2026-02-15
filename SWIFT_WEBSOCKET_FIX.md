# Swift/iOS WebSocket Authentication - Quick Fix

## The Problem

You're trying to set the `Sec-WebSocket-Protocol` header using `setValue(_:forHTTPHeaderField:)`, which **doesn't work** for WebSocket protocol negotiation in Swift.

## ❌ INCORRECT Implementation

```swift
// This DOES NOT work!
var request = URLRequest(url: url)
request.setValue("access_token", forHTTPHeaderField: "Sec-WebSocket-Protocol")
request.setValue(token, forHTTPHeaderField: "Sec-WebSocket-Protocol")  // Overwrites previous value
```

**Why this doesn't work:**
1. Calling `setValue` twice overwrites the first value
2. URLSessionWebSocketTask ignores `Sec-WebSocket-Protocol` set via URLRequest headers
3. Protocol negotiation must happen during WebSocket handshake initialization

## ✅ CORRECT Implementation

### Option 1: Using Sec-WebSocket-Protocol (Recommended - Most Secure)

```swift
private func establishConnection() {
    guard let token = token else {
        connectionStateSubject.send(.failed(NetworkError.unauthorized))
        return
    }
    
    // Build WebSocket URL
    let wsScheme = config.environment == .development ? "ws" : "wss"
    let host = config.apiBaseURL
        .replacingOccurrences(of: "http://", with: "")
        .replacingOccurrences(of: "https://", with: "")
    let wsURLString = "\(wsScheme)://\(host)/ws/chat"
    
    guard let url = URL(string: wsURLString) else {
        connectionStateSubject.send(.failed(NetworkError.invalidURL))
        return
    }
    
    // Create URLRequest (no special headers needed)
    var request = URLRequest(url: url)
    request.timeoutInterval = 10
    
    // Create URLSession
    let config = URLSessionConfiguration.default
    let session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
    
    // ✅ CORRECT: Pass token in protocols array
    webSocketTask = session.webSocketTask(with: request, protocols: [token])
    
    // Alternative with Bearer prefix (also supported by server):
    // webSocketTask = session.webSocketTask(with: request, protocols: ["Bearer.\(token)"])
    
    // Start connection
    webSocketTask?.resume()
    
    // Start receiving messages
    receiveMessage()
}
```

### Option 2: Using Query Parameter (Fallback - Less Secure)

```swift
private func establishConnection() {
    guard let token = token else {
        connectionStateSubject.send(.failed(NetworkError.unauthorized))
        return
    }
    
    // Build WebSocket URL with token as query parameter
    let wsScheme = config.environment == .development ? "ws" : "wss"
    let host = config.apiBaseURL
        .replacingOccurrences(of: "http://", with: "")
        .replacingOccurrences(of: "https://", with: "")
    
    // Add token as query parameter
    let wsURLString = "\(wsScheme)://\(host)/ws/chat?token=\(token)"
    
    guard let url = URL(string: wsURLString) else {
        connectionStateSubject.send(.failed(NetworkError.invalidURL))
        return
    }
    
    let request = URLRequest(url: url)
    let config = URLSessionConfiguration.default
    let session = URLSession(configuration: config)
    
    // No protocols needed when using query parameter
    webSocketTask = session.webSocketTask(with: request)
    webSocketTask?.resume()
    
    receiveMessage()
}
```

## Key Differences

| Method | Security | Implementation | URL Visible |
|--------|----------|----------------|-------------|
| **Sec-WebSocket-Protocol** | ✅ Most Secure | `protocols: [token]` | Token NOT in URL |
| **Query Parameter** | ⚠️ Less Secure | `?token=xxx` | Token IN URL (logged) |

## Recommended: Use Sec-WebSocket-Protocol

### Benefits:
- ✅ Token doesn't appear in URL
- ✅ Not logged by proxies/load balancers
- ✅ Not stored in browser/app history
- ✅ Industry best practice
- ✅ Supported by your server

### Implementation:
```swift
// Just pass the token in the protocols array
webSocketTask = session.webSocketTask(with: request, protocols: [token])
```

## Complete Working Example

```swift
import Foundation
import Combine

class WebSocketManager: NSObject {
    private var webSocketTask: URLSessionWebSocketTask?
    private var session: URLSession?
    private let connectionStateSubject = PassthroughSubject<ConnectionState, Never>()
    
    enum ConnectionState {
        case disconnected
        case connecting
        case connected
        case failed(Error)
    }
    
    func connect(url: URL, token: String) {
        connectionStateSubject.send(.connecting)
        
        var request = URLRequest(url: url)
        request.timeoutInterval = 10
        
        let config = URLSessionConfiguration.default
        session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
        
        // ✅ CORRECT: Pass token as protocol
        webSocketTask = session?.webSocketTask(with: request, protocols: [token])
        webSocketTask?.resume()
        
        receiveMessage()
    }
    
    private func receiveMessage() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .success(let message):
                self?.connectionStateSubject.send(.connected)
                switch message {
                case .string(let text):
                    print("Received: \(text)")
                case .data(let data):
                    print("Received data: \(data)")
                @unknown default:
                    break
                }
                self?.receiveMessage()
            case .failure(let error):
                self?.connectionStateSubject.send(.failed(error))
            }
        }
    }
    
    func send(message: String) {
        let wsMessage = URLSessionWebSocketTask.Message.string(message)
        webSocketTask?.send(wsMessage) { error in
            if let error = error {
                print("Send error: \(error)")
            }
        }
    }
    
    func disconnect() {
        webSocketTask?.cancel(with: .goingAway, reason: nil)
        connectionStateSubject.send(.disconnected)
    }
}

extension WebSocketManager: URLSessionWebSocketDelegate {
    func urlSession(_ session: URLSession, 
                    webSocketTask: URLSessionWebSocketTask, 
                    didOpenWithProtocol protocol: String?) {
        print("✅ Connected with protocol: \(protocol ?? "none")")
        connectionStateSubject.send(.connected)
    }
    
    func urlSession(_ session: URLSession, 
                    webSocketTask: URLSessionWebSocketTask, 
                    didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, 
                    reason: Data?) {
        print("❌ Connection closed")
        connectionStateSubject.send(.disconnected)
    }
}
```

## Usage

```swift
let manager = WebSocketManager()
let url = URL(string: "wss://your-domain.com/ws/chat")!
let jwtToken = "eyJhbGciOiJIUzI1NiJ9..."

manager.connect(url: url, token: jwtToken)
```

## Server Support

Your server supports both methods:

1. **Sec-WebSocket-Protocol header** (checked first - recommended)
   - Plain format: `protocols: [token]`
   - Bearer format: `protocols: ["Bearer.\(token)"]`

2. **Query parameters** (fallback)
   - `?token=xxx`
   - `?access_token=xxx`

For more details, see `WEBSOCKET_AUTH.md` in the repository.
