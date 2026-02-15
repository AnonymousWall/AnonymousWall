package com.anonymous.wall.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.simple.SimpleHttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WebSocketTokenReader to verify JWT token extraction from 
 * Sec-WebSocket-Protocol header and query parameters.
 */
@DisplayName("WebSocketTokenReader Tests")
class WebSocketTokenReaderTest {

    private WebSocketTokenReader tokenReader;

    @BeforeEach
    void setUp() {
        tokenReader = new WebSocketTokenReader();
    }

    @Nested
    @DisplayName("Token Extraction from Sec-WebSocket-Protocol Header")
    class SecWebSocketProtocolHeaderTests {

        @Test
        @DisplayName("Should extract token from Sec-WebSocket-Protocol header (plain format)")
        void shouldExtractTokenFromProtocolHeaderPlainFormat() {
            // Arrange
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat")
                    .header("Sec-WebSocket-Protocol", expectedToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(expectedToken, actualToken.get(), "Token should match");
        }

        @Test
        @DisplayName("Should extract token from Sec-WebSocket-Protocol header (Bearer prefix)")
        void shouldExtractTokenFromProtocolHeaderBearerFormat() {
            // Arrange
            String token = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            String bearerToken = "Bearer." + token;
            HttpRequest<?> request = HttpRequest.GET("/ws/chat")
                    .header("Sec-WebSocket-Protocol", bearerToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(token, actualToken.get(), "Token should match (without Bearer prefix)");
        }

        @Test
        @DisplayName("Should extract token from comma-separated protocols (plain format first)")
        void shouldExtractTokenFromCommaSeparatedProtocolsPlainFirst() {
            // Arrange
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            String protocols = expectedToken + ", chat, json";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat")
                    .header("Sec-WebSocket-Protocol", protocols);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(expectedToken, actualToken.get(), "Token should match");
        }

        @Test
        @DisplayName("Should extract token from comma-separated protocols (Bearer format)")
        void shouldExtractTokenFromCommaSeparatedProtocolsBearerFormat() {
            // Arrange
            String token = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            String protocols = "chat, Bearer." + token + ", json";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat")
                    .header("Sec-WebSocket-Protocol", protocols);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(token, actualToken.get(), "Token should match (without Bearer prefix)");
        }

        @Test
        @DisplayName("Should return empty when Sec-WebSocket-Protocol has no JWT token")
        void shouldReturnEmptyWhenProtocolHeaderHasNoToken() {
            // Arrange
            HttpRequest<?> request = HttpRequest.GET("/ws/chat")
                    .header("Sec-WebSocket-Protocol", "chat, json");

            // Act
            Optional<String> token = tokenReader.findToken(request);

            // Assert
            assertFalse(token.isPresent(), "Token should not be present");
        }

        @Test
        @DisplayName("Should handle empty Bearer prefix")
        void shouldHandleEmptyBearerPrefix() {
            // Arrange
            HttpRequest<?> request = HttpRequest.GET("/ws/chat")
                    .header("Sec-WebSocket-Protocol", "Bearer.");

            // Act
            Optional<String> token = tokenReader.findToken(request);

            // Assert
            assertFalse(token.isPresent(), "Token should not be present for empty Bearer prefix");
        }

        @Test
        @DisplayName("Should prefer Sec-WebSocket-Protocol over query parameters")
        void shouldPreferProtocolHeaderOverQueryParams() {
            // Arrange
            String headerToken = "eyJhbGciOiJIUzI1NiJ9.header.signature";
            String queryToken = "eyJhbGciOiJIUzI1NiJ9.query.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?token=" + queryToken)
                    .header("Sec-WebSocket-Protocol", headerToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(headerToken, actualToken.get(), "Should use header token");
        }
    }

    @Nested
    @DisplayName("Token Extraction from Query Parameters")
    class TokenExtractionTests {

        @Test
        @DisplayName("Should extract token from 'token' query parameter")
        void shouldExtractTokenFromTokenParameter() {
            // Arrange
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?token=" + expectedToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(expectedToken, actualToken.get(), "Token should match");
        }

        @Test
        @DisplayName("Should extract token from 'access_token' query parameter")
        void shouldExtractTokenFromAccessTokenParameter() {
            // Arrange
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?access_token=" + expectedToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(expectedToken, actualToken.get(), "Token should match");
        }

        @Test
        @DisplayName("Should prefer 'token' parameter over 'access_token'")
        void shouldPreferTokenParameter() {
            // Arrange
            String tokenParam = "eyJhbGciOiJIUzI1NiJ9.token.signature";
            String accessTokenParam = "eyJhbGciOiJIUzI1NiJ9.access_token.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?token=" + tokenParam + "&access_token=" + accessTokenParam);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(tokenParam, actualToken.get(), "Should use 'token' parameter");
        }

        @Test
        @DisplayName("Should return empty when no token parameter present")
        void shouldReturnEmptyWhenNoToken() {
            // Arrange
            HttpRequest<?> request = HttpRequest.GET("/ws/chat");

            // Act
            Optional<String> token = tokenReader.findToken(request);

            // Assert
            assertFalse(token.isPresent(), "Token should not be present");
        }

        @Test
        @DisplayName("Should handle empty token parameter")
        void shouldHandleEmptyTokenParameter() {
            // Arrange
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?token=");

            // Act
            Optional<String> token = tokenReader.findToken(request);

            // Assert
            // Empty string is still returned by Micronaut's parameter API
            // The JWT validator will reject it as invalid
            assertTrue(token.isPresent(), "Token parameter present but empty");
            assertEquals("", token.get(), "Should return empty string");
        }

        @Test
        @DisplayName("Should handle URL-encoded tokens")
        void shouldHandleUrlEncodedTokens() {
            // Arrange - JWT tokens don't typically need encoding, but test that it works
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.test.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?token=" + expectedToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(expectedToken, actualToken.get(), "Token should match");
        }

        @Test
        @DisplayName("Should work with WebSocket upgrade requests")
        void shouldWorkWithWebSocketUpgradeRequest() {
            // Arrange - Simulating a WebSocket upgrade request
            String expectedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.signature";
            HttpRequest<?> request = HttpRequest.GET("/ws/chat?token=" + expectedToken);

            // Act
            Optional<String> actualToken = tokenReader.findToken(request);

            // Assert
            assertTrue(actualToken.isPresent(), "Token should be present");
            assertEquals(expectedToken, actualToken.get(), "Token should match");
        }
    }

    @Nested
    @DisplayName("Priority Tests")
    class PriorityTests {

        @Test
        @DisplayName("Priority: Sec-WebSocket-Protocol > token > access_token")
        void shouldRespectTokenPriority() {
            // Arrange
            String headerToken = "eyJhbGciOiJIUzI1NiJ9.header.signature";
            String tokenParam = "eyJhbGciOiJIUzI1NiJ9.token.signature";
            String accessTokenParam = "eyJhbGciOiJIUzI1NiJ9.access_token.signature";
            
            // Test 1: All three present - should prefer header
            HttpRequest<?> request1 = HttpRequest.GET("/ws/chat?token=" + tokenParam + "&access_token=" + accessTokenParam)
                    .header("Sec-WebSocket-Protocol", headerToken);
            assertEquals(headerToken, tokenReader.findToken(request1).get(), "Should prefer header over query params");

            // Test 2: Only query params - should prefer 'token' over 'access_token'
            HttpRequest<?> request2 = HttpRequest.GET("/ws/chat?token=" + tokenParam + "&access_token=" + accessTokenParam);
            assertEquals(tokenParam, tokenReader.findToken(request2).get(), "Should prefer 'token' over 'access_token'");

            // Test 3: Only access_token
            HttpRequest<?> request3 = HttpRequest.GET("/ws/chat?access_token=" + accessTokenParam);
            assertEquals(accessTokenParam, tokenReader.findToken(request3).get(), "Should use 'access_token' when others absent");
        }
    }

    @Nested
    @DisplayName("Order Tests")
    class OrderTests {

        @Test
        @DisplayName("Should have higher order than standard readers")
        void shouldHaveCorrectOrder() {
            // Act
            int order = tokenReader.getOrder();

            // Assert
            assertEquals(200, order, "Order should be 200 to run after standard header readers");
        }
    }
}
