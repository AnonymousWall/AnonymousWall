package com.anonymous.wall.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.token.reader.TokenReader;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Custom TokenReader to extract JWT tokens from WebSocket connections.
 * 
 * This is necessary because WebSocket connections initiated from browsers cannot include
 * custom HTTP headers (like Authorization: Bearer). Supports multiple token passing methods:
 * 
 * 1. Sec-WebSocket-Protocol header (most secure - recommended):
 *    - Single protocol: new WebSocket(url, [token])
 *    - Bearer prefix: new WebSocket(url, ['Bearer.' + token])
 * 
 * 2. Query parameters (fallback for compatibility):
 *    - Query parameter "token" (e.g., ws://host/ws/chat?token=xxx)
 *    - Query parameter "access_token" (e.g., ws://host/ws/chat?access_token=xxx)
 * 
 * The Sec-WebSocket-Protocol approach is preferred as tokens don't appear in URLs/logs.
 * The token will be validated by Micronaut's security framework using the same
 * JWT validation logic as regular HTTP requests.
 */
@Singleton
public class WebSocketTokenReader implements TokenReader<HttpRequest<?>> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTokenReader.class);
    private static final String TOKEN_PARAM = "token";
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    private static final String SEC_WEBSOCKET_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";
    private static final String BEARER_PREFIX = "Bearer.";
    
    // High order to run after standard header readers
    public static final Integer ORDER = 200;

    @Override
    public Optional<String> findToken(HttpRequest<?> request) {
        String requestPath = request.getPath();
        
        // First try Sec-WebSocket-Protocol header (most secure)
        Optional<String> token = extractTokenFromProtocolHeader(request, requestPath);
        if (token.isPresent()) {
            return token;
        }
        
        // Fall back to "token" query parameter
        token = request.getParameters().get(TOKEN_PARAM, String.class);
        
        if (token.isPresent()) {
            log.info("WebSocketTokenReader: Found JWT token in '{}' query parameter for path: {}", TOKEN_PARAM, requestPath);
            return token;
        }
        
        // Fall back to "access_token" parameter
        token = request.getParameters().get(ACCESS_TOKEN_PARAM, String.class);
        
        if (token.isPresent()) {
            log.info("WebSocketTokenReader: Found JWT token in '{}' query parameter for path: {}", ACCESS_TOKEN_PARAM, requestPath);
            return token;
        }
        
        // No token found
        log.debug("WebSocketTokenReader: No token found in Sec-WebSocket-Protocol header or query parameters for path: {}", requestPath);
        return Optional.empty();
    }
    
    /**
     * Extracts JWT token from Sec-WebSocket-Protocol header.
     * Supports both plain token and Bearer-prefixed token formats.
     * 
     * @param request The HTTP request
     * @param requestPath The request path for logging
     * @return Optional containing the token if found
     */
    private Optional<String> extractTokenFromProtocolHeader(HttpRequest<?> request, String requestPath) {
        String protocolHeader = request.getHeaders().get(SEC_WEBSOCKET_PROTOCOL_HEADER);
        
        if (protocolHeader == null || protocolHeader.isEmpty()) {
            return Optional.empty();
        }
        
        // The header can contain multiple comma-separated protocols
        String[] protocolList = protocolHeader.split(",");
        
        for (String protocol : protocolList) {
            String trimmedProtocol = protocol.trim();
            
            // Check if it's a Bearer-prefixed token (e.g., "Bearer.eyJhbGc...")
            if (trimmedProtocol.startsWith(BEARER_PREFIX)) {
                String token = trimmedProtocol.substring(BEARER_PREFIX.length());
                if (!token.isEmpty()) {
                    log.info("WebSocketTokenReader: Found JWT token in '{}' header (Bearer format) for path: {}", 
                            SEC_WEBSOCKET_PROTOCOL_HEADER, requestPath);
                    return Optional.of(token);
                }
            }
            
            // Check if it looks like a JWT token (starts with "eyJ" which is base64 encoded "{")
            if (trimmedProtocol.startsWith("eyJ")) {
                log.info("WebSocketTokenReader: Found JWT token in '{}' header (plain format) for path: {}", 
                        SEC_WEBSOCKET_PROTOCOL_HEADER, requestPath);
                return Optional.of(trimmedProtocol);
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public int getOrder() {
        return ORDER;
    }
}
