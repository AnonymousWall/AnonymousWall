package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.entity.NotificationEntity;
import com.anonymous.wall.repository.NotificationRepository;
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

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    private static final String BASE_PATH = "/api/v1/notifications";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    JwtTokenService jwtTokenService;

    private UserEntity recipient;
    private UserEntity actor;
    private String recipientToken;
    private String actorToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        recipient = new UserEntity();
        recipient.setEmail("recipient" + System.currentTimeMillis() + "@harvard.edu");
        recipient.setSchoolDomain("harvard.edu");
        recipient.setVerified(true);
        recipient.setPasswordSet(true);
        recipient = userRepository.save(recipient);
        recipientToken = jwtTokenService.generateToken(recipient);

        actor = new UserEntity();
        actor.setEmail("actor" + System.currentTimeMillis() + "@harvard.edu");
        actor.setSchoolDomain("harvard.edu");
        actor.setVerified(true);
        actor.setPasswordSet(true);
        actor = userRepository.save(actor);
        actorToken = jwtTokenService.generateToken(actor);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ────────────────────────────────────────────────────────────
    // GET /api/v1/notifications
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /notifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should return empty list when no notifications exist")
        void shouldReturnEmptyList() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH)
                            .header("Authorization", "Bearer " + recipientToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            List<?> content = (List<?>) body.get("content");
            assertNotNull(content);
            assertTrue(content.isEmpty());
            assertEquals(0, ((Number) body.get("totalElements")).intValue());
        }

        @Test
        @DisplayName("Should return notifications for authenticated user")
        void shouldReturnNotificationsForUser() {
            UUID entityId = UUID.randomUUID();
            NotificationEntity n = new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", entityId, "Post Title", "Actor Name");
            notificationRepository.save(n);

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH)
                            .header("Authorization", "Bearer " + recipientToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            List<?> content = (List<?>) response.body().get("content");
            assertEquals(1, content.size());

            Map<?, ?> dto = (Map<?, ?>) content.get(0);
            assertEquals("COMMENT", dto.get("type"));
            assertEquals("Post Title", dto.get("entityTitle"));
            assertEquals("Actor Name", dto.get("actorProfileName"));
            assertFalse((Boolean) dto.get("read"));
        }

        @Test
        @DisplayName("Should only return notifications for the authenticated user, not others")
        void shouldNotReturnOtherUsersNotifications() {
            // Notification for recipient
            notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));
            // Notification for actor (different user)
            notificationRepository.save(new NotificationEntity(
                    actor.getId(), recipient.getId(), "COMMENT", UUID.randomUUID(), null, null));

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH)
                            .header("Authorization", "Bearer " + recipientToken),
                    Map.class
            );

            List<?> content = (List<?>) response.body().get("content");
            assertEquals(1, content.size());
        }

        @Test
        @DisplayName("Should return 401 when no auth token provided")
        void shouldReturn401WhenUnauthenticated() {
            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(HttpRequest.GET(BASE_PATH), Map.class));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }

        @Test
        @DisplayName("Should support page and size query parameters")
        void shouldSupportPagination() {
            for (int i = 0; i < 5; i++) {
                notificationRepository.save(new NotificationEntity(
                        recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), "Title " + i, null));
            }

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "?page=1&size=2")
                            .header("Authorization", "Bearer " + recipientToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertEquals(2, ((List<?>) body.get("content")).size());
            assertEquals(5, ((Number) body.get("totalElements")).intValue());
            assertEquals(1, ((Number) body.get("page")).intValue());
        }
    }

    // ────────────────────────────────────────────────────────────
    // GET /api/v1/notifications/unread-count
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /notifications/unread-count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return zero when no unread notifications")
        void shouldReturnZeroWhenNoUnread() {
            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/unread-count")
                            .header("Authorization", "Bearer " + recipientToken),
                    Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertEquals(0L, ((Number) response.body().get("count")).longValue());
        }

        @Test
        @DisplayName("Should return correct unread count")
        void shouldReturnCorrectUnreadCount() {
            notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));
            notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));

            HttpResponse<Map> response = client.toBlocking().exchange(
                    HttpRequest.GET(BASE_PATH + "/unread-count")
                            .header("Authorization", "Bearer " + recipientToken),
                    Map.class
            );

            assertEquals(2L, ((Number) response.body().get("count")).longValue());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() {
            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(HttpRequest.GET(BASE_PATH + "/unread-count"), Map.class));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }
    }

    // ────────────────────────────────────────────────────────────
    // POST /api/v1/notifications/mark-all-read
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /notifications/mark-all-read")
    class MarkAllReadTests {

        @Test
        @DisplayName("Should mark all notifications as read")
        void shouldMarkAllNotificationsAsRead() {
            notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));
            notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));

            HttpResponse<Void> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/mark-all-read", null)
                            .header("Authorization", "Bearer " + recipientToken),
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());

            long unread = notificationRepository.countByRecipientUserIdAndRead(recipient.getId(), false);
            assertEquals(0, unread);
        }

        @Test
        @DisplayName("Should return OK even when no notifications exist")
        void shouldReturnOkWhenNothingToMark() {
            HttpResponse<Void> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/mark-all-read", null)
                            .header("Authorization", "Bearer " + recipientToken),
                    Void.class
            );
            assertEquals(HttpStatus.OK, response.getStatus());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() {
            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH + "/mark-all-read", null), Void.class));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }
    }

    // ────────────────────────────────────────────────────────────
    // POST /api/v1/notifications/{id}/read
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /notifications/{id}/read")
    class MarkSingleReadTests {

        @Test
        @DisplayName("Should mark a single notification as read")
        void shouldMarkSingleNotificationAsRead() {
            NotificationEntity notification = notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));

            HttpResponse<Void> response = client.toBlocking().exchange(
                    HttpRequest.POST(BASE_PATH + "/" + notification.getId() + "/read", null)
                            .header("Authorization", "Bearer " + recipientToken),
                    Void.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            assertTrue(notificationRepository.findById(notification.getId()).map(NotificationEntity::isRead).orElse(false));
        }

        @Test
        @DisplayName("Should return 403 when notification belongs to a different user")
        void shouldReturn403WhenNotOwner() {
            NotificationEntity notification = notificationRepository.save(new NotificationEntity(
                    recipient.getId(), actor.getId(), "COMMENT", UUID.randomUUID(), null, null));

            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH + "/" + notification.getId() + "/read", null)
                                    .header("Authorization", "Bearer " + actorToken),
                            Void.class
                    ));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        @DisplayName("Should return 404 when notification does not exist")
        void shouldReturn404WhenNotFound() {
            UUID randomId = UUID.randomUUID();

            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH + "/" + randomId + "/read", null)
                                    .header("Authorization", "Bearer " + recipientToken),
                            Void.class
                    ));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() {
            HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(
                            HttpRequest.POST(BASE_PATH + "/" + UUID.randomUUID() + "/read", null),
                            Void.class
                    ));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }
    }
}
