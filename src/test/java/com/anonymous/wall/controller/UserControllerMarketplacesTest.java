package com.anonymous.wall.controller;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.JwtTokenService;
import com.anonymous.wall.service.MarketplaceService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("UserController Marketplaces Tests")
class UserControllerMarketplacesTest {

    private static final String BASE_PATH = "/api/v1/users/me/marketplaces";

    @Inject
    @Client("/")
    private HttpClient client;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private JwtTokenService jwtTokenService;

    @Inject
    private MarketplaceService marketplaceService;

    private UserEntity testUser1;
    private UserEntity testUser2;
    private String jwtToken1;
    private String jwtToken2;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        marketplaceItemRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = new UserEntity();
        testUser1.setId(UUID.randomUUID());
        testUser1.setEmail("user1@harvard.edu");
        testUser1.setSchoolDomain("harvard.edu");
        testUser1.setProfileName("User1");
        testUser1.setVerified(true);
        testUser1.setPasswordSet(true);
        testUser1.setPasswordHash("dummy");
        testUser1 = userRepository.save(testUser1);
        jwtToken1 = jwtTokenService.generateToken(testUser1);

        testUser2 = new UserEntity();
        testUser2.setId(UUID.randomUUID());
        testUser2.setEmail("user2@harvard.edu");
        testUser2.setSchoolDomain("harvard.edu");
        testUser2.setProfileName("User2");
        testUser2.setVerified(true);
        testUser2.setPasswordSet(true);
        testUser2.setPasswordHash("dummy");
        testUser2 = userRepository.save(testUser2);
        jwtToken2 = jwtTokenService.generateToken(testUser2);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        marketplaceItemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("User Own Marketplaces - Basic Functionality")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should return user's own marketplace items")
        void shouldReturnUserOwnMarketplaceItems() {
            marketplaceService.createItem(new CreateItemRequest("Laptop", 500.0f), null, testUser1.getId());
            marketplaceService.createItem(new CreateItemRequest("Textbook", 30.0f), null, testUser1.getId());
            marketplaceService.createItem(new CreateItemRequest("Bike", 200.0f), null, testUser2.getId());

            HttpRequest<?> request = HttpRequest.GET(BASE_PATH).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());

            Map<String, Object> body = response.body();
            assertNotNull(body);

            List<Map> data = (List<Map>) body.get("data");
            assertNotNull(data);
            assertEquals(2, data.size(), "User1 should have 2 marketplace items");

            for (Map item : data) {
                Map author = (Map) item.get("author");
                assertEquals(testUser1.getId().toString(), author.get("id"));
            }
        }

        @Test
        @DisplayName("Should return empty list when user has no marketplace items")
        void shouldReturnEmptyListWhenNoItems() {
            HttpRequest<?> request = HttpRequest.GET(BASE_PATH).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertNotNull(data);
            assertEquals(0, data.size());

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(0, pagination.get("total"));
        }

        @Test
        @DisplayName("Should only return user's own marketplace items, not others")
        void shouldOnlyReturnOwnItems() {
            for (int i = 0; i < 3; i++) {
                marketplaceService.createItem(new CreateItemRequest("Item1-" + i, (float) (i + 10)), null, testUser1.getId());
            }
            for (int i = 0; i < 5; i++) {
                marketplaceService.createItem(new CreateItemRequest("Item2-" + i, (float) (i + 20)), null, testUser2.getId());
            }

            HttpRequest<?> request = HttpRequest.GET(BASE_PATH).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);
            List<Map> data = (List<Map>) ((Map<String, Object>) response.body()).get("data");
            assertEquals(3, data.size());

            request = HttpRequest.GET(BASE_PATH).bearerAuth(jwtToken2);
            response = client.toBlocking().exchange(request, Map.class);
            data = (List<Map>) ((Map<String, Object>) response.body()).get("data");
            assertEquals(5, data.size());
        }
    }

    @Nested
    @DisplayName("User Own Marketplaces - Pagination")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate marketplace items correctly")
        void shouldPaginateItemsCorrectly() {
            for (int i = 0; i < 25; i++) {
                marketplaceService.createItem(new CreateItemRequest("Item " + i, (float) (i + 1)), null, testUser1.getId());
            }

            String endpoint = BASE_PATH + "?page=1&limit=20";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(20, data.size());

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(1, pagination.get("page"));
            assertEquals(20, pagination.get("limit"));
            assertEquals(25, pagination.get("total"));
            assertEquals(2, pagination.get("totalPages"));

            endpoint = BASE_PATH + "?page=2&limit=20";
            request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            response = client.toBlocking().exchange(request, Map.class);

            body = response.body();
            data = (List<Map>) body.get("data");
            assertEquals(5, data.size());

            pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(2, pagination.get("page"));
        }
    }

    @Nested
    @DisplayName("User Own Marketplaces - Sorting")
    class SortingTests {

        @Test
        @DisplayName("Should sort marketplace items by newest first (default)")
        void shouldSortByNewestFirst() throws InterruptedException {
            for (int i = 1; i <= 3; i++) {
                marketplaceService.createItem(new CreateItemRequest("Item " + i, (float) i), null, testUser1.getId());
                Thread.sleep(1000);
            }

            String endpoint = BASE_PATH + "?sort=NEWEST";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            assertEquals("Item 3", data.get(0).get("title"));
            assertEquals("Item 2", data.get(1).get("title"));
            assertEquals("Item 1", data.get(2).get("title"));
        }

        @Test
        @DisplayName("Should sort marketplace items by oldest first")
        void shouldSortByOldestFirst() throws InterruptedException {
            for (int i = 1; i <= 3; i++) {
                marketplaceService.createItem(new CreateItemRequest("Item " + i, (float) i), null, testUser1.getId());
                Thread.sleep(1000);
            }

            String endpoint = BASE_PATH + "?sort=OLDEST";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(3, data.size());

            assertEquals("Item 1", data.get(0).get("title"));
            assertEquals("Item 2", data.get(1).get("title"));
            assertEquals("Item 3", data.get(2).get("title"));
        }
    }

    @Nested
    @DisplayName("User Own Marketplaces - Hidden Items")
    class HiddenItemsTests {

        @Test
        @DisplayName("Should exclude hidden marketplace items")
        void shouldExcludeHiddenItems() {
            marketplaceService.createItem(new CreateItemRequest("Laptop", 500.0f), null, testUser1.getId());
            MarketplaceItem item2 = marketplaceService.createItem(
                new CreateItemRequest("Textbook", 30.0f), null, testUser1.getId());
            marketplaceService.createItem(new CreateItemRequest("Bike", 200.0f), null, testUser1.getId());

            item2.setHidden(true);
            marketplaceItemRepository.update(item2);

            HttpRequest<?> request = HttpRequest.GET(BASE_PATH).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            Map<String, Object> body = response.body();
            List<Map> data = (List<Map>) body.get("data");
            assertEquals(2, data.size(), "Should only return non-hidden marketplace items");

            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(2, pagination.get("total"));
        }
    }

    @Nested
    @DisplayName("User Own Marketplaces - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle invalid page parameter")
        void shouldHandleInvalidPageParameter() {
            marketplaceService.createItem(new CreateItemRequest("Laptop", 500.0f), null, testUser1.getId());

            String endpoint = BASE_PATH + "?page=0";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());

            Map<String, Object> body = response.body();
            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(1, pagination.get("page"));
        }

        @Test
        @DisplayName("Should handle limit out of bounds")
        void shouldHandleLimitOutOfBounds() {
            marketplaceService.createItem(new CreateItemRequest("Laptop", 500.0f), null, testUser1.getId());

            String endpoint = BASE_PATH + "?limit=200";
            HttpRequest<?> request = HttpRequest.GET(endpoint).bearerAuth(jwtToken1);
            HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

            assertEquals(HttpStatus.OK, response.getStatus());

            Map<String, Object> body = response.body();
            Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
            assertEquals(20, pagination.get("limit"));
        }
    }
}
