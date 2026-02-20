package com.anonymous.wall.admin.controller;

import com.anonymous.wall.entity.ChatMessage;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.ChatMessageRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Chat Controller Tests")
class AdminChatControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    ChatMessageRepository chatMessageRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/admin/conversations";

    private UserEntity adminUser;
    private UserEntity user1;
    private UserEntity user2;
    private UUID conversationId;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAll();

        adminUser = new UserEntity();
        adminUser.setEmail("admin" + System.currentTimeMillis() + "@test.edu");
        adminUser.setSchoolDomain("test.edu");
        adminUser.setVerified(true);
        adminUser.setPasswordSet(true);
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenService.generateToken(adminUser);

        user1 = new UserEntity();
        user1.setEmail("user1_" + System.currentTimeMillis() + "@test.edu");
        user1.setSchoolDomain("test.edu");
        user1.setVerified(true);
        user1.setPasswordSet(true);
        user1.setRole("USER");
        user1 = userRepository.save(user1);
        userToken = jwtTokenService.generateToken(user1);

        user2 = new UserEntity();
        user2.setEmail("user2_" + System.currentTimeMillis() + "@test.edu");
        user2.setSchoolDomain("test.edu");
        user2.setVerified(true);
        user2.setPasswordSet(true);
        user2.setRole("USER");
        user2 = userRepository.save(user2);

        conversationId = ConversationIdGenerator.generate(user1.getId(), user2.getId());
        ChatMessage msg = new ChatMessage(user1.getId(), user2.getId(), conversationId, "Hello");
        chatMessageRepository.save(msg);
    }

    @AfterEach
    void tearDown() {
        chatMessageRepository.deleteAll();
    }

    @Nested
    @DisplayName("List Conversations Endpoint Tests")
    class ListConversationsTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can list all conversations")
        void adminCanListConversations() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH).bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            List<?> data = (List<?>) response.body().get("data");
            assertNotNull(data);
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot list conversations via admin endpoint")
        void regularUserCannotListConversations() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH).bearerAuth(userToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("Negative: Unauthenticated cannot list conversations")
        void unauthenticatedCannotListConversations() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH),
                    Map.class
                )
            );
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Get Conversation Messages Endpoint Tests")
    class GetMessagesTests {

        @Test
        @Order(1)
        @DisplayName("Positive: Admin can get messages in a conversation")
        void adminCanGetConversationMessages() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(BASE_PATH + "/" + conversationId + "/messages").bearerAuth(adminToken),
                Map.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
            assertNotNull(response.body());
            List<?> data = (List<?>) response.body().get("data");
            assertNotNull(data);
            assertFalse(data.isEmpty());
        }

        @Test
        @Order(2)
        @DisplayName("Negative: Regular user cannot get conversation messages via admin endpoint")
        void regularUserCannotGetConversationMessages() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/" + conversationId + "/messages").bearerAuth(userToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }
}
