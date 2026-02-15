package com.anonymous.wall.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.token.reader.TokenReader;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Custom TokenReader to extract JWT tokens from query parameters for WebSocket connections.
 * 
 * This is necessary because WebSocket connections initiated from browsers cannot include
 * custom HTTP headers (like Authorization: Bearer). The standard approach is to pass
 * the JWT token as a query parameter instead.
 * 
 * Supports token extraction from:
 * - Query parameter "token" (e.g., ws://host/ws/chat?token=xxx)
 * - Query parameter "access_token" (e.g., ws://host/ws/chat?access_token=xxx)
 * 
 * The token will be validated by Micronaut's security framework using the same
 * JWT validation logic as regular HTTP requests.
 */
@Singleton
public class WebSocketTokenReader implements TokenReader<HttpRequest<?>> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketTokenReader.class);
    private static final String TOKEN_PARAM = "token";
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    
    // High order to run after standard header readers
    public static final Integer ORDER = 200;

    @Override
    public Optional<String> findToken(HttpRequest<?> request) {
        String requestPath = request.getPath();
        
        // First try "token" parameter
        Optional<String> token = request.getParameters().get(TOKEN_PARAM, String.class);
        
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
        
        // No token found in query parameters
        log.debug("WebSocketTokenReader: No token found in query parameters for path: {}", requestPath);
        return Optional.empty();
    }
    
    @Override
    public int getOrder() {
        return ORDER;
    }
}
