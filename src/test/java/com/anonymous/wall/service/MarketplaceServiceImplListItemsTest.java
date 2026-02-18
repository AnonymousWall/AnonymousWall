package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("MarketplaceServiceImpl - List Items Tests")
class MarketplaceServiceImplListItemsTest {

    @Inject
    private MarketplaceService marketplaceService;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clean up
        marketplaceItemRepository.deleteAll();

        // Create test user
        testUser = new UserEntity();
        testUser.setEmail("testuser" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("List Items - Positive Cases")
    class ListItemsPositiveTests {

        @Test
        @DisplayName("Should list items with default sorting (newest)")
        void shouldListItemsWithDefaultSorting() throws InterruptedException {
            // Arrange - Create 3 items
            CreateItemRequest request1 = new CreateItemRequest("Item 1", 10f);
            request1.setDescription("First");
            CreateItemRequest request2 = new CreateItemRequest("Item 2", 20f);
            request2.setDescription("Second");
            CreateItemRequest request3 = new CreateItemRequest("Item 3", 30f);
            request3.setDescription("Third");

            marketplaceService.createItem(request1, testUser.getId());
            Thread.sleep(100);
            marketplaceService.createItem(request2, testUser.getId());
            Thread.sleep(100);
            MarketplaceItem item3 = marketplaceService.createItem(request3, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "newest");

            // Assert
            assertEquals(3, result.getTotalSize());
            List<MarketplaceItem> items = result.getContent();
            assertEquals(3, items.size());
            // Newest first
            assertEquals(item3.getId(), items.get(0).getId());
        }

        @Test
        @DisplayName("Should list items sorted by price ascending")
        void shouldListItemsSortedByPriceAsc() {
            // Arrange
            CreateItemRequest request1 = new CreateItemRequest("Expensive", 100f);
            request1.setDescription("High price");
            CreateItemRequest request2 = new CreateItemRequest("Medium", 50f);
            request2.setDescription("Mid price");
            CreateItemRequest request3 = new CreateItemRequest("Cheap", 10f);
            request3.setDescription("Low price");

            marketplaceService.createItem(request1, testUser.getId());
            marketplaceService.createItem(request2, testUser.getId());
            MarketplaceItem cheapItem = marketplaceService.createItem(request3, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "price-asc");

            // Assert
            List<MarketplaceItem> items = result.getContent();
            assertEquals(3, items.size());
            // Cheapest first
            assertEquals(cheapItem.getId(), items.get(0).getId());
            assertTrue(items.get(0).getPrice().floatValue() <= items.get(1).getPrice().floatValue());
            assertTrue(items.get(1).getPrice().floatValue() <= items.get(2).getPrice().floatValue());
        }

        @Test
        @DisplayName("Should list items sorted by price descending")
        void shouldListItemsSortedByPriceDesc() {
            // Arrange
            CreateItemRequest request1 = new CreateItemRequest("Cheap", 10f);
            request1.setDescription("Low");
            CreateItemRequest request2 = new CreateItemRequest("Medium", 50f);
            request2.setDescription("Mid");
            CreateItemRequest request3 = new CreateItemRequest("Expensive", 100f);
            request3.setDescription("High");

            marketplaceService.createItem(request1, testUser.getId());
            marketplaceService.createItem(request2, testUser.getId());
            MarketplaceItem expensiveItem = marketplaceService.createItem(request3, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "price-desc");

            // Assert
            List<MarketplaceItem> items = result.getContent();
            assertEquals(3, items.size());
            // Most expensive first
            assertEquals(expensiveItem.getId(), items.get(0).getId());
            assertTrue(items.get(0).getPrice().floatValue() >= items.get(1).getPrice().floatValue());
            assertTrue(items.get(1).getPrice().floatValue() >= items.get(2).getPrice().floatValue());
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePagination() {
            // Arrange - Create 5 items
            for (int i = 1; i <= 5; i++) {
                CreateItemRequest request = new CreateItemRequest("Item " + i, (float) i * 10);
                request.setDescription("Description " + i);
                marketplaceService.createItem(request, testUser.getId());
            }

            // Act - Get page 1 with size 2
            Pageable pageable1 = Pageable.from(0, 2);
            Page<MarketplaceItem> page1 = marketplaceService.listItems(pageable1, "newest");

            // Act - Get page 2 with size 2
            Pageable pageable2 = Pageable.from(1, 2);
            Page<MarketplaceItem> page2 = marketplaceService.listItems(pageable2, "newest");

            // Assert
            assertEquals(5, page1.getTotalSize());
            assertEquals(2, page1.getContent().size());
            assertEquals(5, page2.getTotalSize());
            assertEquals(2, page2.getContent().size());
            assertEquals(3, page1.getTotalPages());
        }

        @Test
        @DisplayName("Should return empty list when no items exist")
        void shouldReturnEmptyListWhenNoItems() {
            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "newest");

            // Assert
            assertEquals(0, result.getTotalSize());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should handle null sortBy parameter")
        void shouldHandleNullSortBy() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setDescription("Test");
            marketplaceService.createItem(request, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, null);

            // Assert
            assertEquals(1, result.getTotalSize());
        }

        @Test
        @DisplayName("Should handle invalid sortBy parameter")
        void shouldHandleInvalidSortBy() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setDescription("Test");
            marketplaceService.createItem(request, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "invalid-sort");

            // Assert - Should default to newest
            assertEquals(1, result.getTotalSize());
        }
    }

    @Nested
    @DisplayName("List Items - Boundary Cases")
    class ListItemsBoundaryTests {

        @Test
        @DisplayName("Should handle large page size")
        void shouldHandleLargePageSize() {
            // Arrange - Create 3 items
            for (int i = 1; i <= 3; i++) {
                CreateItemRequest request = new CreateItemRequest("Item " + i, (float) i);
                request.setDescription("Desc");
                marketplaceService.createItem(request, testUser.getId());
            }

            // Act
            Pageable pageable = Pageable.from(0, 100);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "newest");

            // Assert
            assertEquals(3, result.getTotalSize());
            assertEquals(3, result.getContent().size());
        }

        @Test
        @DisplayName("Should handle page beyond available items")
        void shouldHandlePageBeyondAvailable() {
            // Arrange
            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setDescription("Test");
            marketplaceService.createItem(request, testUser.getId());

            // Act - Request page 10
            Pageable pageable = Pageable.from(10, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "newest");

            // Assert
            assertEquals(1, result.getTotalSize());
            assertTrue(result.getContent().isEmpty());
        }
    }
}
