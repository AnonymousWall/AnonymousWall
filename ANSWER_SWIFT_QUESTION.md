# Answer to Swift WebSocket Authentication Question

## Your Question
> What is the correct way to implement this Swift code for using Sec-WebSocket-Protocol for request.setValue?

## The Problem with Your Code

```swift
// ❌ INCORRECT - This doesn't work
var request = URLRequest(url: url)
request.setValue("access_token", forHTTPHeaderField: "Sec-WebSocket-Protocol")
request.setValue(token, forHTTPHeaderField: "Sec-WebSocket-Protocol")  // Overwrites previous line!
```

**Issues:**
1. The second `setValue` overwrites the first one
2. URLSessionWebSocketTask **ignores** the Sec-WebSocket-Protocol header when set via URLRequest
3. Protocol negotiation must happen in the WebSocketTask initializer

## ✅ Correct Solution

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
    
    // Create URLRequest (no Sec-WebSocket-Protocol header needed here)
    var request = URLRequest(url: url)
    request.timeoutInterval = 10
    
    // Create URLSession
    let config = URLSessionConfiguration.default
    let session = URLSession(configuration: config)
    
    // ✅ CORRECT: Pass token in the protocols array
    webSocketTask = session.webSocketTask(with: request, protocols: [token])
    
    // Start connection
    webSocketTask?.resume()
    
    // Start receiving messages
    receiveMessage()
}
```

## Key Change

**Instead of:**
```swift
request.setValue(token, forHTTPHeaderField: "Sec-WebSocket-Protocol")
```

**Use:**
```swift
webSocketTask = session.webSocketTask(with: request, protocols: [token])
```

## Why This Works

The `protocols` parameter in `webSocketTask(with:protocols:)` is specifically designed for WebSocket protocol negotiation. It properly sets the `Sec-WebSocket-Protocol` header during the WebSocket handshake.

## Alternative: Query Parameter (Less Secure)

If you can't use the protocols array for some reason, you can fall back to query parameters:

```swift
// Add token to URL
let wsURLString = "\(wsScheme)://\(host)/ws/chat?token=\(token)"
guard let url = URL(string: wsURLString) else { return }

let request = URLRequest(url: url)
webSocketTask = session.webSocketTask(with: request)  // No protocols parameter
webSocketTask?.resume()
```

**Note:** This is less secure because the token appears in the URL and may be logged.

## Complete Working Example

See `SWIFT_WEBSOCKET_FIX.md` for a complete, production-ready implementation with error handling and delegate methods.

## Server Support

Your server now supports both methods:
1. **Sec-WebSocket-Protocol header** (recommended, most secure)
2. **Query parameters** `?token=xxx` or `?access_token=xxx` (fallback)

The server checks in this priority order:
1. Sec-WebSocket-Protocol header (checked first)
2. token query parameter
3. access_token query parameter

## Summary

**Don't use `setValue` for Sec-WebSocket-Protocol in Swift.**

**Instead, use:**
```swift
webSocketTask = session.webSocketTask(with: request, protocols: [token])
```

This is the correct, Apple-supported way to do WebSocket protocol negotiation with JWT authentication.
