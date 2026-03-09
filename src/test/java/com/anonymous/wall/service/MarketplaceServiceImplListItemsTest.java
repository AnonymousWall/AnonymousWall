package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.model.CreateItemRequestCategory;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.MarketplaceService;
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
        void shouldListItemsWithDefaultSorting() {
            // Arrange - Create 3 items
            CreateItemRequest request1 = new CreateItemRequest("Item 1", 10f);
            request1.setDescription("First");
            CreateItemRequest request2 = new CreateItemRequest("Item 2", 20f);
            request2.setDescription("Second");
            CreateItemRequest request3 = new CreateItemRequest("Item 3", 30f);
            request3.setDescription("Third");

            marketplaceService.createItem(request1, null, testUser.getId());
            marketplaceService.createItem(request2, null, testUser.getId());
            marketplaceService.createItem(request3, null, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "newest");

            // Assert
            assertEquals(3, result.getTotalSize());
            List<MarketplaceItem> items = result.getContent();
            assertEquals(3, items.size());
            // Verify items are sorted by created_at descending (newest first)
            assertTrue(items.get(0).getCreatedAt().compareTo(items.get(1).getCreatedAt()) >= 0);
            assertTrue(items.get(1).getCreatedAt().compareTo(items.get(2).getCreatedAt()) >= 0);
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

            marketplaceService.createItem(request1, null, testUser.getId());
            marketplaceService.createItem(request2, null, testUser.getId());
            MarketplaceItem cheapItem = marketplaceService.createItem(request3, null, testUser.getId());

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

            marketplaceService.createItem(request1, null, testUser.getId());
            marketplaceService.createItem(request2, null, testUser.getId());
            MarketplaceItem expensiveItem = marketplaceService.createItem(request3, null, testUser.getId());

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
                marketplaceService.createItem(request, null, testUser.getId());
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
            marketplaceService.createItem(request, null, testUser.getId());

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
            marketplaceService.createItem(request, null, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.listItems(pageable, "invalid-sort");

            // Assert - Should default to newest
            assertEquals(1, result.getTotalSize());
        }
    }

    @Nested
    @DisplayName("List Items - Category Filter")
    class ListItemsCategoryFilterTests {

        @Test
        @DisplayName("Should filter items by category")
        void shouldFilterItemsByCategory() {
            // Arrange - Create items with different categories
            CreateItemRequest electronics1 = new CreateItemRequest("Laptop", 500f);
            electronics1.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(electronics1, null, testUser.getId());

            CreateItemRequest electronics2 = new CreateItemRequest("Phone", 300f);
            electronics2.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(electronics2, null, testUser.getId());

            CreateItemRequest textbook = new CreateItemRequest("Math Book", 30f);
            textbook.setCategory(CreateItemRequestCategory.TEXTBOOKS);
            marketplaceService.createItem(textbook, null, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                "campus", pageable, testUser.getId(), "test.edu", "newest", "electronics");

            // Assert - should only return electronics items
            assertEquals(2, result.getTotalSize());
            assertTrue(result.getContent().stream()
                .allMatch(item -> "electronics".equals(item.getCategory())));
        }

        @Test
        @DisplayName("Should return all items when no category filter")
        void shouldReturnAllItemsWithoutCategoryFilter() {
            // Arrange
            CreateItemRequest req1 = new CreateItemRequest("Laptop", 500f);
            req1.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req1, null, testUser.getId());

            CreateItemRequest req2 = new CreateItemRequest("Math Book", 30f);
            req2.setCategory(CreateItemRequestCategory.TEXTBOOKS);
            marketplaceService.createItem(req2, null, testUser.getId());

            CreateItemRequest req3 = new CreateItemRequest("Chair", 50f);
            req3.setCategory(CreateItemRequestCategory.FURNITURE);
            marketplaceService.createItem(req3, null, testUser.getId());

            // Act - no category filter
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                "campus", pageable, testUser.getId(), "test.edu", "newest", null);

            // Assert - should return all items
            assertEquals(3, result.getTotalSize());
        }

        @Test
        @DisplayName("Should sort filtered items by price ascending")
        void shouldSortFilteredItemsByPriceAsc() {
            // Arrange
            CreateItemRequest cheap = new CreateItemRequest("Basic Phone", 100f);
            cheap.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(cheap, null, testUser.getId());

            CreateItemRequest expensive = new CreateItemRequest("Gaming PC", 2000f);
            expensive.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(expensive, null, testUser.getId());

            CreateItemRequest mid = new CreateItemRequest("Tablet", 500f);
            mid.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(mid, null, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                "campus", pageable, testUser.getId(), "test.edu", "price-asc", "electronics");

            // Assert
            assertEquals(3, result.getTotalSize());
            List<MarketplaceItem> items = result.getContent();
            assertTrue(items.get(0).getPrice().floatValue() <= items.get(1).getPrice().floatValue());
            assertTrue(items.get(1).getPrice().floatValue() <= items.get(2).getPrice().floatValue());
        }

        @Test
        @DisplayName("Should sort filtered items by price descending")
        void shouldSortFilteredItemsByPriceDesc() {
            // Arrange
            CreateItemRequest cheap = new CreateItemRequest("Basic Phone", 100f);
            cheap.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(cheap, null, testUser.getId());

            CreateItemRequest expensive = new CreateItemRequest("Gaming PC", 2000f);
            expensive.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(expensive, null, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                "campus", pageable, testUser.getId(), "test.edu", "price-desc", "electronics");

            // Assert
            assertEquals(2, result.getTotalSize());
            List<MarketplaceItem> items = result.getContent();
            assertTrue(items.get(0).getPrice().floatValue() >= items.get(1).getPrice().floatValue());
        }

        @Test
        @DisplayName("Should return empty when no items match category")
        void shouldReturnEmptyWhenNoCategoryMatch() {
            // Arrange
            CreateItemRequest req = new CreateItemRequest("Laptop", 500f);
            req.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req, null, testUser.getId());

            // Act
            Pageable pageable = Pageable.from(0, 10);
            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                "campus", pageable, testUser.getId(), "test.edu", "newest", "furniture");

            // Assert
            assertEquals(0, result.getTotalSize());
            assertTrue(result.getContent().isEmpty());
        }
    }
}
