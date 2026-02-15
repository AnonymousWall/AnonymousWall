# WebSocket Authentication Troubleshooting Guide

This guide helps diagnose WebSocket authentication issues by explaining the authentication flow and what logs to expect.

## Authentication Flow

When a client connects to `/ws/chat`, the following happens in order:

1. **Token Extraction** (`WebSocketTokenReader`)
   - Checks Sec-WebSocket-Protocol header first (recommended)
   - Falls back to query parameters if header not found
2. **JWT Validation** (Micronaut Security)
3. **Blocked User Check** (`BlockedUserFilter`)
4. **WebSocket Connection** (`ChatWebSocketHandler.onOpen()`)

## Expected Logs (in order)

### 1. Token Found

#### From Sec-WebSocket-Protocol Header (Recommended)
```
INFO  com.anonymous.wall.security.WebSocketTokenReader - WebSocketTokenReader: Found JWT token in 'Sec-WebSocket-Protocol' header (plain format) for path: /ws/chat
```
or
```
INFO  com.anonymous.wall.security.WebSocketTokenReader - WebSocketTokenReader: Found JWT token in 'Sec-WebSocket-Protocol' header (Bearer format) for path: /ws/chat
```
✅ **If you see this**: Token is being extracted correctly from the Sec-WebSocket-Protocol header (most secure method).

#### From Query Parameter (Fallback)
```
INFO  com.anonymous.wall.security.WebSocketTokenReader - WebSocketTokenReader: Found JWT token in 'token' query parameter for path: /ws/chat
```
✅ **If you see this**: Token is being extracted correctly from the query parameter.

❌ **If you don't see either log**: 
- **Using header method**: Check that you're passing the token in the second parameter: `new WebSocket(url, [token])`
- **Using query param**: Check if the URL has the `?token=xxx` query parameter
- Make sure you're quoting the URL in the shell: `wscat -c 'ws://...'`
- Verify the server is running

### 2. Blocked User Check
```
DEBUG com.anonymous.wall.security.BlockedUserFilter - BlockedUserFilter: User {userId} is not blocked, allowing access to path: /ws/chat
```
✅ **If you see this**: User is authenticated and not blocked.

⚠️ **If you see "Blocked user attempted to access"**:
```
WARN  com.anonymous.wall.security.BlockedUserFilter - BlockedUserFilter: Blocked user attempted to access: userId={userId}, path=/ws/chat
```
Your account has been blocked by an administrator. Contact support.

❌ **If you see "No authenticated principal"**:
```
DEBUG com.anonymous.wall.security.BlockedUserFilter - BlockedUserFilter: No authenticated principal for path: /ws/chat
```
JWT validation failed. See "Common JWT Validation Failures" below.

### 3. WebSocket Connection Opened
```
INFO  com.anonymous.wall.controller.ChatWebSocketHandler - WebSocket connection opened for user: {userId}, session: {sessionId}
```
✅ **If you see this**: Success! You're connected.

❌ **If you don't see this**: Connection was rejected before reaching the handler.

## Common HTTP Response Codes

### 401 Unauthorized
**Cause**: JWT token validation failed before authentication.

**Common reasons**:
- Token is expired (check `exp` claim)
- Token signature is invalid (wrong secret key)
- Token format is malformed

**What to check**:
- Generate a fresh token from `/api/auth/login`
- Verify the `JWT_GENERATOR_SIGNATURE_SECRET` matches between token generation and validation
- Check if token has expired (JWT tokens typically expire after 24 hours)

### 403 Forbidden  
**Cause**: Authentication succeeded, but access is denied.

**Common reasons**:
1. **User is blocked**: Your account has been blocked by an administrator
2. **CORS issue**: Origin header doesn't match allowed origins (browser only)

**What to check**:
- Look for "Blocked user attempted to access" in logs
- Check if your account status in the database
- For browser clients, ensure CORS is configured correctly

## Debugging Steps

### Step 1: Enable DEBUG logging
Add to `application-dev.properties`:
```properties
logging.level.com.anonymous.wall.security=DEBUG
logging.level.io.micronaut.security=DEBUG
```

### Step 2: Test with a fresh token

#### Using Sec-WebSocket-Protocol Header (Recommended)
```bash
# 1. Login to get a fresh token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' \
  | jq -r '.accessToken')

# 2. Use the token immediately via Sec-WebSocket-Protocol header
wscat -c ws://localhost:8080/ws/chat --subprotocol "$TOKEN"
```

#### Using Query Parameter (Fallback)
```bash
# 1. Login to get a fresh token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"your@email.com","password":"yourpassword"}' \
  | jq -r '.accessToken'

# 2. Use the token immediately
wscat -c 'ws://localhost:8080/ws/chat?token=YOUR_FRESH_TOKEN_HERE'
```

### Step 3: Decode the JWT token
Use https://jwt.io to decode your token and check:
- `sub` claim contains valid user UUID
- `exp` claim is in the future (Unix timestamp)
- `iss` claim is "anonymouswall"
- `roles` claim contains ["USER"] or appropriate role

### Step 4: Check the logs in order
1. Look for `WebSocketTokenReader: Found JWT token in 'Sec-WebSocket-Protocol' header` or `'token' query parameter` - confirms token extraction
2. Look for `BlockedUserFilter:` messages - confirms authentication status
3. Look for `WebSocket connection opened` - confirms successful connection

## Example: Successful Connection (Sec-WebSocket-Protocol)

```bash
$ wscat -c ws://localhost:8080/ws/chat --subprotocol 'eyJhbGc...'
Connected (press CTRL+C to quit)
< {"type":"connected","userId":"303af0ec-7846-42e5-b654-f605f33632bf","timestamp":1771230449000}
```

**Server logs:**
```
INFO  WebSocketTokenReader: Found JWT token in 'Sec-WebSocket-Protocol' header (plain format) for path: /ws/chat
DEBUG BlockedUserFilter: User 303af0ec-7846-42e5-b654-f605f33632bf is not blocked, allowing access to path: /ws/chat
INFO  WebSocket connection opened for user: 303af0ec-7846-42e5-b654-f605f33632bf, session: abc123
```

## Example: Successful Connection (Query Parameter)

```bash
$ wscat -c 'ws://localhost:8080/ws/chat?token=eyJhbGc...'
Connected (press CTRL+C to quit)
< {"type":"connected","userId":"303af0ec-7846-42e5-b654-f605f33632bf","timestamp":1771230449000}
```

**Server logs:**
```
INFO  WebSocketTokenReader: Found JWT token in 'token' query parameter for path: /ws/chat
DEBUG BlockedUserFilter: User 303af0ec-7846-42e5-b654-f605f33632bf is not blocked, allowing access to path: /ws/chat
INFO  WebSocket connection opened for user: 303af0ec-7846-42e5-b654-f605f33632bf, session: abc123
```

## Example: Failed Connection (Expired Token)

```bash
$ wscat -c 'ws://localhost:8080/ws/chat?token=eyJhbGc...'
error: Unexpected server response: 401
```

**Server logs:**
```
INFO  WebSocketTokenReader: Found JWT token in 'token' query parameter for path: /ws/chat
DEBUG BlockedUserFilter: No authenticated principal for path: /ws/chat
```

**Solution**: Token is expired or invalid. Get a fresh token from `/api/auth/login`.

## Example: Failed Connection (Blocked User)

```bash
$ wscat -c 'ws://localhost:8080/ws/chat?token=eyJhbGc...'
error: Unexpected server response: 403
```

**Server logs:**
```
INFO  WebSocketTokenReader: Found JWT token in 'token' query parameter for path: /ws/chat
WARN  BlockedUserFilter: Blocked user attempted to access: userId=303af0ec-7846-42e5-b654-f605f33632bf, path=/ws/chat
```

**Solution**: Contact support - your account has been blocked.

## Still Having Issues?

If you're still seeing errors after following this guide:

1. **Share the complete log output** including all three components:
   - WebSocketTokenReader logs
   - BlockedUserFilter logs  
   - ChatWebSocketHandler logs (if any)

2. **Share the decoded JWT token** (use jwt.io):
   - Subject (sub) claim
   - Expiration (exp) claim
   - Issued at (iat) claim
   - Roles claim

3. **Verify the database state**:
   - Check if user exists: `SELECT * FROM user_entity WHERE id = 'your-user-id'`
   - Check if user is blocked: `SELECT blocked FROM user_entity WHERE id = 'your-user-id'`

4. **Check the configuration**:
   - Verify `JWT_GENERATOR_SIGNATURE_SECRET` is set correctly
   - Verify `micronaut.security.authentication=bearer` in application.properties
