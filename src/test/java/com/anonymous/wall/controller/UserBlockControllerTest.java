package com.anonymous.wall.controller;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserBlockRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("UserBlock Controller Integration Tests")
class UserBlockControllerTest {

    private static final String BLOCKS_PATH = "/api/v1/users/me/blocks";

    @Inject
    @Client("/")
    private HttpClient client;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserBlockRepository userBlockRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userBlockRepository.deleteAll();
        userRepository.deleteAll();

        userA = new UserEntity();
        userA.setEmail("usera" + System.currentTimeMillis() + "@harvard.edu");
        userA.setSchoolDomain("harvard.edu");
        userA.setProfileName("UserA");
        userA.setVerified(true);
        userA.setPasswordSet(true);
        userA.setPasswordHash("dummy");
        userA = userRepository.save(userA);
        tokenA = jwtTokenService.generateToken(userA);

        userB = new UserEntity();
        userB.setEmail("userb" + System.currentTimeMillis() + "@harvard.edu");
        userB.setSchoolDomain("harvard.edu");
        userB.setProfileName("UserB");
        userB.setVerified(true);
        userB.setPasswordSet(true);
        userB.setPasswordHash("dummy");
        userB = userRepository.save(userB);
        tokenB = jwtTokenService.generateToken(userB);
    }

    @AfterEach
    void tearDown() {
        userBlockRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ================= POST /me/blocks/{targetUserId} =================

    @Test
    @DisplayName("Positive: Block user successfully")
    void shouldBlockUserSuccessfully() {
        HttpRequest<?> request = HttpRequest.POST(
                BLOCKS_PATH + "/" + userB.getId(), null)
                .bearerAuth(tokenA);

        var response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        Map<?, ?> body = response.body();
        assertNotNull(body);
        assertEquals("User blocked successfully", body.get("message"));
    }

    @Test
    @DisplayName("Negative: Should fail when blocking self")
    void shouldFailWhenBlockingSelf() {
        HttpRequest<?> request = HttpRequest.POST(
                BLOCKS_PATH + "/" + userA.getId(), null)
                .bearerAuth(tokenA);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Negative: Should fail when unauthenticated")
    void shouldFailBlockWhenUnauthenticated() {
        HttpRequest<?> request = HttpRequest.POST(
                BLOCKS_PATH + "/" + userB.getId(), null);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        assertTrue(ex.getStatus() == HttpStatus.UNAUTHORIZED || ex.getStatus() == HttpStatus.FORBIDDEN);
    }

    // ================= DELETE /me/blocks/{targetUserId} =================

    @Test
    @DisplayName("Positive: Unblock user successfully")
    void shouldUnblockUserSuccessfully() {
        // First block
        client.toBlocking().exchange(
                HttpRequest.POST(BLOCKS_PATH + "/" + userB.getId(), null).bearerAuth(tokenA), Map.class);

        // Then unblock
        HttpRequest<?> request = HttpRequest.DELETE(
                BLOCKS_PATH + "/" + userB.getId())
                .bearerAuth(tokenA);

        var response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        Map<?, ?> body = response.body();
        assertNotNull(body);
        assertEquals("User unblocked successfully", body.get("message"));
    }

    @Test
    @DisplayName("Negative: Should fail when unblocking a user that is not blocked")
    void shouldFailWhenUnblockingNonBlockedUser() {
        HttpRequest<?> request = HttpRequest.DELETE(
                BLOCKS_PATH + "/" + userB.getId())
                .bearerAuth(tokenA);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // ================= GET /me/blocks =================

    @Test
    @DisplayName("Positive: Get block list successfully")
    void shouldGetBlockListSuccessfully() {
        // Block userB first
        client.toBlocking().exchange(
                HttpRequest.POST(BLOCKS_PATH + "/" + userB.getId(), null).bearerAuth(tokenA), Map.class);

        HttpRequest<?> request = HttpRequest.GET(BLOCKS_PATH).bearerAuth(tokenA);
        var response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        Map<?, ?> body = response.body();
        assertNotNull(body);
        var data = body.get("data");
        assertNotNull(data);
        assertTrue(data instanceof java.util.List);
        assertEquals(1, ((java.util.List<?>) data).size());
    }

    @Test
    @DisplayName("Positive: Get empty block list when no blocks exist")
    void shouldGetEmptyBlockList() {
        HttpRequest<?> request = HttpRequest.GET(BLOCKS_PATH).bearerAuth(tokenA);
        var response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        Map<?, ?> body = response.body();
        assertNotNull(body);
        var data = body.get("data");
        assertNotNull(data);
        assertTrue(data instanceof java.util.List);
        assertEquals(0, ((java.util.List<?>) data).size());
    }
}
