package com.anonymous.wall.controller;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.MarketplaceItemRepository;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("Marketplace Controller - Hide/Unhide Item Tests")
class MarketplaceControllerHideItemTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    @Inject
    MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    private static final String BASE_PATH = "/api/v1/marketplace";

    private UserEntity testUser;
    private UserEntity otherUser;
    private String jwtToken;
    private String otherJwtToken;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setEmail("seller" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser.setProfileName("TestSeller");
        testUser = userRepository.save(testUser);
        jwtToken = jwtTokenService.generateToken(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("buyer" + System.currentTimeMillis() + "@test.edu");
        otherUser.setSchoolDomain("test.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser.setProfileName("TestBuyer");
        otherUser = userRepository.save(otherUser);
        otherJwtToken = jwtTokenService.generateToken(otherUser);

        marketplaceItemRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Hide Item - PATCH /marketplace/{id}/hide")
    class HideItemTests {

        @Test
        @DisplayName("Should hide item successfully")
        void shouldHideItemSuccessfully() {
            MarketplaceItem item = new MarketplaceItem(testUser.getId(), "Old Textbook", null, new BigDecimal("20.00"), null, null);
            item.setSchoolDomain("test.edu");
            item.setWall("campus");
            final MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(BASE_PATH + "/" + savedItem.getId() + "/hide", null)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue(body.get("message").toString().contains("hidden successfully"));

            MarketplaceItem hidden = marketplaceItemRepository.findById(savedItem.getId()).orElseThrow();
            assertTrue(hidden.isHidden());
        }

        @Test
        @DisplayName("Should return 404 when item not found")
        void shouldReturn404WhenItemNotFound() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + UUID.randomUUID() + "/hide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when not owner")
        void shouldReturn403WhenNotOwner() {
            MarketplaceItem item = new MarketplaceItem(otherUser.getId(), "Old Textbook", null, new BigDecimal("20.00"), null, null);
            item.setSchoolDomain("test.edu");
            item.setWall("campus");
            final MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + savedItem.getId() + "/hide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Unhide Item - PATCH /marketplace/{id}/unhide")
    class UnhideItemTests {

        @Test
        @DisplayName("Should unhide item successfully")
        void shouldUnhideItemSuccessfully() {
            MarketplaceItem item = new MarketplaceItem(testUser.getId(), "Old Textbook", null, new BigDecimal("20.00"), null, null);
            item.setSchoolDomain("test.edu");
            item.setWall("campus");
            item.setHidden(true);
            final MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PATCH(BASE_PATH + "/" + savedItem.getId() + "/unhide", null)
                    .header("Authorization", "Bearer " + jwtToken),
                Map.class
            );

            assertEquals(HttpStatus.OK, response.getStatus());
            Map body = response.body();
            assertNotNull(body);
            assertTrue(body.get("message").toString().contains("unhidden successfully"));

            MarketplaceItem unhidden = marketplaceItemRepository.findById(savedItem.getId()).orElseThrow();
            assertFalse(unhidden.isHidden());
        }

        @Test
        @DisplayName("Should return 404 when item not found")
        void shouldReturn404WhenItemNotFound() {
            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + UUID.randomUUID() + "/unhide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("Should return 403 when not owner")
        void shouldReturn403WhenNotOwner() {
            MarketplaceItem item = new MarketplaceItem(otherUser.getId(), "Old Textbook", null, new BigDecimal("20.00"), null, null);
            item.setSchoolDomain("test.edu");
            item.setWall("campus");
            item.setHidden(true);
            final MarketplaceItem savedItem = marketplaceItemRepository.save(item);

            HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                    HttpRequest.PATCH(BASE_PATH + "/" + savedItem.getId() + "/unhide", null)
                        .header("Authorization", "Bearer " + jwtToken),
                    Map.class
                )
            );
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }
}
