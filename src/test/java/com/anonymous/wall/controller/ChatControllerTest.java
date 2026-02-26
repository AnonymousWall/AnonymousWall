package com.anonymous.wall.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.model.SendMessageRequest;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ChatController REST endpoints.
 * Tests the full HTTP request/response cycle with authentication.
 */
@MicronautTest(transactional = false)
@DisplayName("ChatController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    ChatMessageRepository chatMessageRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/chat";

    private UserEntity testUser1;
    private UserEntity testUser2;
    private UserEntity blockedUser;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        // Clean up test data
        chatMessageRepository.deleteAll();

        // Create test users
        testUser1 = new UserEntity();
        testUser1.setEmail("chattest1_" + System.currentTimeMillis() + "@harvard.edu");
        testUser1.setProfileName("ChatTestUser1");
        testUser1.setSchoolDomain("harvard.edu");
        testUser1.setVerified(true);
        testUser1.setBlocked(false);
        testUser1 = userRepository.save(testUser1);

        testUser2 = new UserEntity();
        testUser2.setEmail("chattest2_" + System.currentTimeMillis() + "@harvard.edu");
        testUser2.setProfileName("ChatTestUser2");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setVerified(true);
        testUser2.setBlocked(false);
        testUser2 = userRepository.save(testUser2);

        blockedUser = new UserEntity();
        blockedUser.setEmail("blocked_" + System.currentTimeMillis() + "@harvard.edu");
        blockedUser.setProfileName("BlockedUser");
        blockedUser.setSchoolDomain("harvard.edu");
        blockedUser.setVerified(true);
        blockedUser.setBlocked(true);
        blockedUser = userRepository.save(blockedUser);

        // Generate JWT tokens
        user1Token = jwtTokenService.generateToken(testUser1);

        user2Token = jwtTokenService.generateToken(testUser2);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        chatMessageRepository.deleteAll();
        if (testUser1 != null) userRepository.deleteById(testUser1.getId());
        if (testUser2 != null) userRepository.deleteById(testUser2.getId());
        if (blockedUser != null) userRepository.deleteById(blockedUser.getId());
    }

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @Order(1)
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            // Arrange
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Hello, this is a test message!");

            // Act
            HttpResponse<ChatMessageDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/messages", request)
                    .bearerAuth(user1Token),
                ChatMessageDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertNotNull(response.body());
            
            ChatMessageDTO message = response.body();
            assertEquals(testUser1.getId(), message.getSenderId());
            assertEquals(testUser2.getId(), message.getReceiverId());
            assertEquals("Hello, this is a test message!", message.getContent());
            assertFalse(message.getReadStatus());
        }

        @Test
        @Order(2)
        @DisplayName("Should fail to send message without authentication")
        void shouldFailWithoutAuth() {
            // Arrange
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Test message");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request)
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("Should fail to send message to blocked user")
        void shouldFailToSendToBlockedUser() {
            // Arrange
            SendMessageRequest request = new SendMessageRequest(blockedUser.getId());
            request.setContent("Test message");

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request)
                        .bearerAuth(user1Token),
                    ChatMessageDTO.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @Order(4)
        @DisplayName("Should fail when neither content nor imageUrl is provided")
        void shouldFailWithNeitherContentNorImageUrl() {
            // Arrange - request with no content and no imageUrl
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request)
                        .bearerAuth(user1Token),
                    ChatMessageDTO.class
                )
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @Order(5)
        @DisplayName("Should send image-only message successfully")
        void shouldSendImageOnlyMessageSuccessfully() {
            // Arrange
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setImageUrl("https://example.com/chat/image.jpg");

            // Act
            HttpResponse<ChatMessageDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/messages", request)
                    .bearerAuth(user1Token),
                ChatMessageDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertNotNull(response.body());
            ChatMessageDTO message = response.body();
            assertEquals("https://example.com/chat/image.jpg", message.getImageUrl());
        }

        @Test
        @Order(6)
        @DisplayName("Should send message with both content and imageUrl")
        void shouldSendMessageWithContentAndImageUrl() {
            // Arrange
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Check this image!");
            request.setImageUrl("https://example.com/chat/image.jpg");

            // Act
            HttpResponse<ChatMessageDTO> response = client.toBlocking().exchange(
                HttpRequest.POST(BASE_PATH + "/messages", request)
                    .bearerAuth(user1Token),
                ChatMessageDTO.class
            );

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertNotNull(response.body());
            ChatMessageDTO message = response.body();
            assertEquals("Check this image!", message.getContent());
            assertEquals("https://example.com/chat/image.jpg", message.getImageUrl());
        }
    }

    @Nested
    @DisplayName("Get Message History Tests")
    class GetMessageHistoryTests {

        @Test
        @Order(5)
        @DisplayName("Should get message history successfully")
        void shouldGetMessageHistorySuccessfully() {
            // Arrange - create some messages
            ChatMessage msg1 = new ChatMessage(testUser1.getId(), testUser2.getId(), "Message 1");
            ChatMessage msg2 = new ChatMessage(testUser2.getId(), testUser1.getId(), "Message 2");
            ChatMessage msg3 = new ChatMessage(testUser1.getId(), testUser2.getId(), "Message 3");
            chatMessageRepository.save(msg1);
            chatMessageRepository.save(msg2);
            chatMessageRepository.save(msg3);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/messages/" + testUser2.getId())
                    .bearerAuth(user1Token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            
            Map<String, Object> body = response.body();
            assertTrue(body.containsKey("messages"));
            
            @SuppressWarnings("unchecked")
            List<Map> messages = (List<Map>) body.get("messages");
            assertEquals(3, messages.size());
        }

        @Test
        @Order(6)
        @DisplayName("Should return empty list when no messages exist")
        void shouldReturnEmptyList() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/messages/" + testUser2.getId())
                    .bearerAuth(user1Token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            
            Map<String, Object> body = response.body();
            @SuppressWarnings("unchecked")
            List<Map> messages = (List<Map>) body.get("messages");
            assertEquals(0, messages.size());
        }

        @Test
        @Order(7)
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/messages/" + testUser2.getId())
                )
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Get Conversations Tests")
    class GetConversationsTests {

        @Test
        @Order(8)
        @DisplayName("Should get conversations successfully")
        void shouldGetConversationsSuccessfully() {
            // Arrange - create messages with user2
            ChatMessage msg1 = new ChatMessage(testUser2.getId(), testUser1.getId(), "Hello!");
            chatMessageRepository.save(msg1);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/conversations")
                    .bearerAuth(user1Token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            
            Map<String, Object> body = response.body();
            assertTrue(body.containsKey("conversations"));
            
            @SuppressWarnings("unchecked")
            List<Map> conversations = (List<Map>) body.get("conversations");
            assertEquals(1, conversations.size());
            
            Map<String, Object> conversation = conversations.get(0);
            assertEquals("ChatTestUser2", conversation.get("profileName"));
        }

        @Test
        @Order(9)
        @DisplayName("Should return empty conversations when none exist")
        void shouldReturnEmptyConversations() {
            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/conversations")
                    .bearerAuth(user1Token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            
            @SuppressWarnings("unchecked")
            List<Map> conversations = (List<Map>) response.body().get("conversations");
            assertEquals(0, conversations.size());
        }
    }

    @Nested
    @DisplayName("Mark Message As Read Tests")
    class MarkMessageAsReadTests {

        @Test
        @Order(10)
        @DisplayName("Should mark message as read successfully")
        void shouldMarkMessageAsReadSuccessfully() {
            // Arrange
            ChatMessage message = new ChatMessage(testUser1.getId(), testUser2.getId(), "Test");
            message.setReadStatus(false);
            message = chatMessageRepository.save(message);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT(BASE_PATH + "/messages/" + message.getId() + "/read", null)
                    .bearerAuth(user2Token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
            
            // Verify message was marked as read
            ChatMessage updated = chatMessageRepository.findById(message.getId()).orElse(null);
            assertNotNull(updated);
            assertTrue(updated.isReadStatus());
        }

        @Test
        @Order(11)
        @DisplayName("Should fail when user is not the receiver")
        void shouldFailWhenNotReceiver() {
            // Arrange
            ChatMessage message = new ChatMessage(testUser1.getId(), testUser2.getId(), "Test");
            ChatMessage savedMessage = chatMessageRepository.save(message);
            UUID messageId = savedMessage.getId();

            // Act & Assert
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/messages/" + messageId + "/read", null)
                        .bearerAuth(user1Token),
                    Map.class
                )
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Mark Conversation As Read Tests")
    class MarkConversationAsReadTests {

        @Test
        @Order(12)
        @DisplayName("Should mark all messages in conversation as read")
        void shouldMarkConversationAsRead() {
            // Arrange
            ChatMessage msg1 = new ChatMessage(testUser1.getId(), testUser2.getId(), "Message 1");
            ChatMessage msg2 = new ChatMessage(testUser1.getId(), testUser2.getId(), "Message 2");
            msg1.setReadStatus(false);
            msg2.setReadStatus(false);
            chatMessageRepository.save(msg1);
            chatMessageRepository.save(msg2);

            // Act
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT(BASE_PATH + "/conversations/" + testUser1.getId() + "/read", null)
                    .bearerAuth(user2Token),
                Map.class
            );

            // Assert
            assertEquals(HttpStatus.OK, response.getStatus());
        }
    }
}
