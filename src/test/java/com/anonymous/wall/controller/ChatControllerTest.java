package com.anonymous.wall.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.Conversation;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.ChatMessageDTO;
import com.anonymous.wall.model.SendMessageRequest;
import com.anonymous.wall.repository.ChatMessageRepository;
import com.anonymous.wall.repository.ConversationRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.util.ConversationIdGenerator;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
    ConversationRepository conversationRepository;

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
        conversationRepository.deleteAll();
        chatMessageRepository.deleteAll();

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

        user1Token = jwtTokenService.generateToken(testUser1);
        user2Token = jwtTokenService.generateToken(testUser2);
    }

    @AfterEach
    void tearDown() {
        conversationRepository.deleteAll();
        chatMessageRepository.deleteAll();
        if (testUser1 != null) userRepository.deleteById(testUser1.getId());
        if (testUser2 != null) userRepository.deleteById(testUser2.getId());
        if (blockedUser != null) userRepository.deleteById(blockedUser.getId());
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    /**
     * Directly inserts a conversation row for user1 — simulates what sendMessage would do.
     * Use this instead of creating ChatMessage rows directly for getConversations tests.
     */
    private void seedConversation(UUID userId, UUID partnerId, String partnerProfileName,
                                  String lastContent, int unreadCount) {
        UUID convId = ConversationIdGenerator.generate(userId, partnerId);
        Conversation conv = new Conversation();
        conv.setConversationId(convId);
        conv.setUserId(userId);
        conv.setPartnerId(partnerId);
        conv.setPartnerProfileName(partnerProfileName);
        conv.setLastMessageContent(lastContent);
        conv.setLastMessageSenderId(partnerId);
        conv.setLastMessageReceiverId(userId);
        conv.setLastMessageReadStatus(false);
        conv.setLastMessageAt(OffsetDateTime.now());
        conv.setUnreadCount(unreadCount);
        conversationRepository.save(conv);
    }

    // ─── Send Message ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @Order(1)
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Hello, this is a test message!");

            HttpResponse<ChatMessageDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request).bearerAuth(user1Token),
                    ChatMessageDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            ChatMessageDTO message = response.body();
            assertNotNull(message);
            assertEquals(testUser1.getId(), message.getSenderId());
            assertEquals(testUser2.getId(), message.getReceiverId());
            assertEquals("Hello, this is a test message!", message.getContent());
            assertFalse(message.getReadStatus());
        }

        @Test
        @Order(2)
        @DisplayName("Should create conversation rows after sending message")
        void shouldCreateConversationRowsAfterSend() {
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Hello!");

            client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request).bearerAuth(user1Token),
                    ChatMessageDTO.class
            );

            UUID convId = ConversationIdGenerator.generate(testUser1.getId(), testUser2.getId());
            assertTrue(conversationRepository.findByConversationIdAndUserId(convId, testUser1.getId()).isPresent());
            assertTrue(conversationRepository.findByConversationIdAndUserId(convId, testUser2.getId()).isPresent());
        }

        @Test
        @Order(3)
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() {
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Test");

            assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH + "/messages", request), ChatMessageDTO.class));
        }

        @Test
        @Order(4)
        @DisplayName("Should fail to send message to blocked user")
        void shouldFailToSendToBlockedUser() {
            SendMessageRequest request = new SendMessageRequest(blockedUser.getId());
            request.setContent("Test");

            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH + "/messages", request).bearerAuth(user1Token),
                            ChatMessageDTO.class));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        @Order(5)
        @DisplayName("Should send image-only message")
        void shouldSendImageOnlyMessage() {
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setImageObjectName("chat/test-image.jpg");

            HttpResponse<ChatMessageDTO> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request).bearerAuth(user1Token),
                    ChatMessageDTO.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatus());
            assertEquals("chat/test-image.jpg", response.body().getImageUrl());
        }
    }

    // ─── Get Message History ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Message History Tests")
    class GetMessageHistoryTests {

        @Test
        @Order(6)
        @DisplayName("Should get message history successfully")
        void shouldGetMessageHistorySuccessfully() {
            chatMessageRepository.save(new ChatMessage(testUser1.getId(), testUser2.getId(), "Message 1"));
            chatMessageRepository.save(new ChatMessage(testUser2.getId(), testUser1.getId(), "Message 2"));
            chatMessageRepository.save(new ChatMessage(testUser1.getId(), testUser2.getId(), "Message 3"));

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/messages/" + testUser2.getId()).bearerAuth(user1Token),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> messages = (List<Map>) response.body().get("messages");
            assertEquals(3, messages.size());
        }

        @Test
        @Order(7)
        @DisplayName("Should return empty list when no messages")
        void shouldReturnEmptyList() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/messages/" + testUser2.getId()).bearerAuth(user1Token),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> messages = (List<Map>) response.body().get("messages");
            assertEquals(0, messages.size());
        }
    }

    // ─── Get Conversations ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Conversations Tests")
    class GetConversationsTests {

        @Test
        @Order(8)
        @DisplayName("Should get conversations from conversation table")
        void shouldGetConversationsSuccessfully() {
            // Seed conversation row directly — not via chat_messages
            seedConversation(testUser1.getId(), testUser2.getId(), "ChatTestUser2", "Hello!", 1);

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/conversations").bearerAuth(user1Token),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> conversations = (List<Map>) response.body().get("conversations");
            assertEquals(1, conversations.size());
            assertEquals("ChatTestUser2", conversations.get(0).get("profileName"));
        }

        @Test
        @Order(9)
        @DisplayName("Should return empty conversations when none exist")
        void shouldReturnEmptyConversations() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/conversations").bearerAuth(user1Token),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<Map> conversations = (List<Map>) response.body().get("conversations");
            assertEquals(0, conversations.size());
        }

        @Test
        @Order(10)
        @DisplayName("Should populate conversation rows when message is sent via API")
        void shouldPopulateConversationOnSend() {
            SendMessageRequest request = new SendMessageRequest(testUser2.getId());
            request.setContent("Hi!");
            client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/messages", request).bearerAuth(user1Token),
                    ChatMessageDTO.class
            );

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/conversations").bearerAuth(user1Token),
                    Map.class
            );

            List<Map> conversations = (List<Map>) response.body().get("conversations");
            assertEquals(1, conversations.size());
        }
    }

    // ─── Mark Message As Read ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Mark Message As Read Tests")
    class MarkMessageAsReadTests {

        @Test
        @Order(11)
        @DisplayName("Should mark message as read successfully")
        void shouldMarkMessageAsReadSuccessfully() {
            ChatMessage message = new ChatMessage(testUser1.getId(), testUser2.getId(), "Test");
            message.setReadStatus(false);
            message = chatMessageRepository.save(message);

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/messages/" + message.getId() + "/read", null)
                            .bearerAuth(user2Token),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(chatMessageRepository.findById(message.getId()).orElseThrow().isReadStatus());
        }

        @Test
        @Order(12)
        @DisplayName("Should fail when user is not the receiver")
        void shouldFailWhenNotReceiver() {
            ChatMessage message = chatMessageRepository.save(
                    new ChatMessage(testUser1.getId(), testUser2.getId(), "Test"));
            UUID messageId = message.getId();

            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                    client.toBlocking().exchange(
                            HttpRequest.PUT(BASE_PATH + "/messages/" + messageId + "/read", null)
                                    .bearerAuth(user1Token),
                            Map.class));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }
    }

    // ─── Mark Conversation As Read ─────────────────────────────────────────────

    @Nested
    @DisplayName("Mark Conversation As Read Tests")
    class MarkConversationAsReadTests {

        @Test
        @Order(13)
        @DisplayName("Should mark conversation as read and reset unread count")
        void shouldMarkConversationAsRead() {
            chatMessageRepository.save(new ChatMessage(testUser1.getId(), testUser2.getId(), "Msg 1"));
            chatMessageRepository.save(new ChatMessage(testUser1.getId(), testUser2.getId(), "Msg 2"));
            seedConversation(testUser2.getId(), testUser1.getId(), "ChatTestUser1", "Msg 2", 2);

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.PUT(BASE_PATH + "/conversations/" + testUser1.getId() + "/read", null)
                            .bearerAuth(user2Token),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());

            UUID convId = ConversationIdGenerator.generate(testUser2.getId(), testUser1.getId());
            conversationRepository.findByConversationIdAndUserId(convId, testUser2.getId())
                    .ifPresent(c -> assertEquals(0, c.getUnreadCount()));
        }
    }
}