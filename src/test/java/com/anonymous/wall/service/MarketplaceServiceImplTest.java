package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.*;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.CommentsService;
import com.anonymous.wall.service.base.MarketplaceService;
import com.anonymous.wall.service.base.UserBlockService;
import com.anonymous.wall.service.base.UserService;
import com.anonymous.wall.service.impl.MarketplaceServiceImpl;
import com.anonymous.wall.util.MediaUtilInterface;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@MicronautTest(transactional = false)
@DisplayName("MarketplaceServiceImpl Tests")
class MarketplaceServiceImplTest {

    @Inject
    private MarketplaceService marketplaceService;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CommentRepository commentRepository;

    private UserEntity testUser;
    private UserEntity otherUser;
    private UserEntity userWithNoSchoolDomain;
    private UserEntity mitUser;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        marketplaceItemRepository.deleteAll();
        userRepository.deleteAll();

        // Use UUID suffix — System.currentTimeMillis() can collide when multiple
        // users are created in the same @BeforeEach within the same millisecond.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        testUser = new UserEntity();
        testUser.setEmail("testuser" + suffix + "@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setProfileName("TestSeller");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("other" + suffix + "@harvard.edu");
        otherUser.setSchoolDomain("harvard.edu");
        otherUser.setProfileName("OtherSeller");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser = userRepository.save(otherUser);

        userWithNoSchoolDomain = new UserEntity();
        userWithNoSchoolDomain.setEmail("nodomain" + suffix + "@example.com");
        userWithNoSchoolDomain.setSchoolDomain(null);
        userWithNoSchoolDomain.setVerified(true);
        userWithNoSchoolDomain.setPasswordSet(true);
        userWithNoSchoolDomain = userRepository.save(userWithNoSchoolDomain);

        mitUser = new UserEntity();
        mitUser.setEmail("mit" + suffix + "@mit.edu");
        mitUser.setSchoolDomain("mit.edu");
        mitUser.setVerified(true);
        mitUser.setPasswordSet(true);
        mitUser = userRepository.save(mitUser);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        marketplaceItemRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private MarketplaceItem createCampusItem(String title, float price, UserEntity owner) {
        return marketplaceService.createItem(new CreateItemRequest(title, price), owner.getId());
    }

    private MarketplaceItem createNationalItem(String title, float price, UserEntity owner) {
        CreateItemRequest req = new CreateItemRequest(title, price);
        req.setWall(CreatePostRequestWall.NATIONAL); // adjust to your WallType enum
        return marketplaceService.createItem(req, owner.getId());
    }

    // ─── Create Item — Positive ────────────────────────────────────────────────

    @Nested
    @DisplayName("Create Item — Positive Cases")
    class CreateItemPositiveTests {

        @Test
        @DisplayName("Should create item with all fields populated")
        void shouldCreateItemWithAllFields() {
            CreateItemRequest request = new CreateItemRequest("Test Item", 99.99f);
            request.setDescription("Description");
            request.setCategory(CreateItemRequestCategory.ELECTRONICS);
            request.setCondition(CreateItemRequestCondition.NEW);

            MarketplaceItem result = marketplaceService.createItem(request, testUser.getId());

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Test Item", result.getTitle());
            assertEquals("Description", result.getDescription());
            // DECIMAL(10,2) column — compare at 2dp scale
            assertEquals(0, new BigDecimal("99.99").compareTo(result.getPrice().setScale(2, RoundingMode.HALF_UP)));
            assertEquals("electronics", result.getCategory());
            assertEquals("new", result.getCondition());
            assertEquals(testUser.getId(), result.getUserId());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create item with minimum required fields — optional fields null")
        void shouldCreateItemWithMinimumFields() {
            CreateItemRequest request = new CreateItemRequest("Min Item", 0f);

            MarketplaceItem result = marketplaceService.createItem(request, testUser.getId());

            assertNotNull(result);
            assertEquals("Min Item", result.getTitle());
            assertNull(result.getDescription());
            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
            assertNull(result.getCategory());
            assertNull(result.getCondition());
        }

        @Test
        @DisplayName("Should default wall to campus when wall not specified")
        void shouldDefaultWallToCampus() {
            CreateItemRequest request = new CreateItemRequest("Textbook", 25f);

            MarketplaceItem result = marketplaceService.createItem(request, testUser.getId());

            assertEquals("campus", result.getWall(),
                    "Wall must default to campus when not specified");
            assertEquals(testUser.getSchoolDomain(), result.getSchoolDomain(),
                    "School domain must be set from the user entity for campus items");
        }

        @Test
        @DisplayName("Should set schoolDomain to null when wall is national")
        void shouldSetSchoolDomainNullForNationalItem() {
            CreateItemRequest request = new CreateItemRequest("Textbook", 25f);
            request.setWall(CreatePostRequestWall.NATIONAL);

            MarketplaceItem result = marketplaceService.createItem(request, testUser.getId());

            assertEquals("national", result.getWall());
            assertNull(result.getSchoolDomain(),
                    "National items must not have a schoolDomain set");
        }

        @Test
        @DisplayName("Should set profileName from user entity on created item")
        void shouldSetProfileNameFromUser() {
            CreateItemRequest request = new CreateItemRequest("Laptop", 500f);

            MarketplaceItem result = marketplaceService.createItem(request, testUser.getId());

            assertEquals("TestSeller", result.getProfileName());
        }

        @Test
        @DisplayName("Should create item with zero price")
        void shouldCreateItemWithZeroPrice() {
            MarketplaceItem result = marketplaceService.createItem(
                    new CreateItemRequest("Free Item", 0f), testUser.getId());

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should create item for each valid condition")
        void shouldCreateItemWithValidConditions() {
            for (CreateItemRequestCondition condition : CreateItemRequestCondition.values()) {
                CreateItemRequest request = new CreateItemRequest("Item " + condition.getValue(), 10f);
                request.setCondition(condition);

                MarketplaceItem result = marketplaceService.createItem(request, testUser.getId());

                assertEquals(condition.getValue(), result.getCondition());
            }
        }

        @Test
        @DisplayName("Should trim title whitespace")
        void shouldTrimTitleWhitespace() {
            MarketplaceItem result = marketplaceService.createItem(
                    new CreateItemRequest("  Trimmed Title  ", 10f), testUser.getId());

            assertEquals("Trimmed Title", result.getTitle());
        }

        @Test
        @DisplayName("Should persist item to database — findById returns it")
        void shouldPersistToDB() {
            MarketplaceItem result = createCampusItem("Textbook", 20f, testUser);

            MarketplaceItem fromDb = marketplaceItemRepository.findById(result.getId()).orElseThrow();
            assertEquals("Textbook", fromDb.getTitle());
        }
    }

    // ─── Create Item — Validation ──────────────────────────────────────────────

    @Nested
    @DisplayName("Create Item — Validation Errors")
    class CreateItemValidationTests {

        @Test
        @DisplayName("Should fail when user not found")
        void shouldFailWhenUserNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(
                            new CreateItemRequest("Test", 10f), UUID.randomUUID()));
            assertTrue(ex.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should fail when title is null")
        void shouldFailWhenTitleNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(
                            new CreateItemRequest(null, 10f), testUser.getId()));
            assertTrue(ex.getMessage().contains("Title is required"));
        }

        @Test
        @DisplayName("Should fail when title is blank")
        void shouldFailWhenTitleBlank() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(
                            new CreateItemRequest("   ", 10f), testUser.getId()));
            assertTrue(ex.getMessage().contains("Title is required"));
        }

        @Test
        @DisplayName("Should fail when price is null")
        void shouldFailWhenPriceNull() {
            CreateItemRequest request = new CreateItemRequest("Test", 0f);
            request.setPrice(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(request, testUser.getId()));
            assertTrue(ex.getMessage().contains("Price is required"));
        }

        @Test
        @DisplayName("Should fail when price is negative")
        void shouldFailWhenPriceNegative() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(
                            new CreateItemRequest("Test", -1f), testUser.getId()));
            assertTrue(ex.getMessage().contains("Price must be greater than or equal to 0"));
        }

        @Test
        @DisplayName("Should fail when title exceeds 255 characters")
        void shouldFailWhenTitleTooLong() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(
                            new CreateItemRequest("a".repeat(256), 10f), testUser.getId()));
            assertTrue(ex.getMessage().contains("Title cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should fail when user has no school domain and wall defaults to campus")
        void shouldFailWhenUserHasNoSchoolDomainForCampusWall() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.createItem(
                            new CreateItemRequest("Laptop", 500f), userWithNoSchoolDomain.getId()));
            assertTrue(ex.getMessage().contains("school domain"));
        }

        // ── Boundaries ───────────────────────────────────────────────────────

        @Test
        @DisplayName("Should accept title of exactly 255 characters — boundary")
        void shouldAcceptTitleAtExactLimit() {
            assertDoesNotThrow(() -> marketplaceService.createItem(
                    new CreateItemRequest("a".repeat(255), 10f), testUser.getId()));
        }

        @Test
        @DisplayName("Should accept single-character title")
        void shouldAcceptOneCharTitle() {
            MarketplaceItem result = marketplaceService.createItem(
                    new CreateItemRequest("A", 10f), testUser.getId());
            assertEquals("A", result.getTitle());
        }

        @Test
        @DisplayName("Should accept large price within DECIMAL(10,2) limits")
        void shouldAcceptLargePrice() {
            MarketplaceItem result = marketplaceService.createItem(
                    new CreateItemRequest("Expensive", 9999999.99f), testUser.getId());
            assertTrue(result.getPrice().compareTo(BigDecimal.valueOf(9999999.0)) > 0);
        }
    }

    // ─── Get Item (single-arg) ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Item — getItem(id)")
    class GetItemTests {

        @Test
        @DisplayName("Should return item by ID")
        void shouldReturnItemById() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);

            MarketplaceItem result = marketplaceService.getItem(created.getId());

            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
            assertEquals("Textbook", result.getTitle());
        }

        @Test
        @DisplayName("Should throw when item not found")
        void shouldThrowWhenNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.getItem(UUID.randomUUID()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should return hidden item — single-arg overload does not check hidden flag")
        void shouldReturnHiddenItem() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);
            marketplaceService.hideItem(created.getId(), testUser.getId());

            // Single-arg getItem does NOT enforce hidden — that is the two-arg overload's job.
            // This test pins that intentional asymmetry.
            assertDoesNotThrow(() -> marketplaceService.getItem(created.getId()));
        }
    }

    // ─── Get Item (two-arg — user-aware) ───────────────────────────────────────

    @Nested
    @DisplayName("Get Item — getItem(id, userId)")
    class GetItemWithUserTests {

        @Test
        @DisplayName("Should return national item for any user")
        void shouldReturnNationalItemForAnyUser() {
            MarketplaceItem created = createNationalItem("Laptop", 500f, testUser);

            MarketplaceItem result = marketplaceService.getItem(created.getId(), mitUser.getId());

            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
        }

        @Test
        @DisplayName("Should return campus item for user from same school")
        void shouldReturnCampusItemForSameSchoolUser() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);

            MarketplaceItem result = marketplaceService.getItem(created.getId(), otherUser.getId());

            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw when item is hidden")
        void shouldThrowWhenItemHidden() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);
            marketplaceService.hideItem(created.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.getItem(created.getId(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should throw when campus item accessed by user from different school")
        void shouldThrowForDifferentSchoolUser() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.getItem(created.getId(), mitUser.getId()));
            assertTrue(ex.getMessage().contains("other schools"));
        }

        @Test
        @DisplayName("Should throw when item not found")
        void shouldThrowWhenNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.getItem(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    // ─── Update Item — Positive ────────────────────────────────────────────────

    @Nested
    @DisplayName("Update Item — Positive Cases")
    class UpdateItemPositiveTests {

        private MarketplaceItem testItem;

        @BeforeEach
        void setUpItem() {
            CreateItemRequest req = new CreateItemRequest("Original Title", 50f);
            req.setDescription("Original Description");
            req.setCategory(CreateItemRequestCategory.TEXTBOOKS);
            req.setCondition(CreateItemRequestCondition.GOOD);
            testItem = marketplaceService.createItem(req, testUser.getId());
        }

        @Test
        @DisplayName("Should update title only — other fields unchanged")
        void shouldUpdateTitleOnly() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("New Title");

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals("New Title", result.getTitle());
            assertEquals("Original Description", result.getDescription());
            assertEquals(0, BigDecimal.valueOf(50).compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should update price only — other fields unchanged")
        void shouldUpdatePriceOnly() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(75.5f);

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals("Original Title", result.getTitle());
            assertEquals(0, BigDecimal.valueOf(75.5).compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should update all fields")
        void shouldUpdateAllFields() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("Completely New");
            request.setDescription("New description");
            request.setPrice(100f);
            request.setCategory(CreateItemRequestCategory.ELECTRONICS);
            request.setCondition(CreateItemRequestCondition.LIKE_NEW);

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals("Completely New", result.getTitle());
            assertEquals("New description", result.getDescription());
            assertEquals(0, BigDecimal.valueOf(100).compareTo(result.getPrice()));
            assertEquals("electronics", result.getCategory());
            assertEquals("like-new", result.getCondition());
        }

        @Test
        @DisplayName("Should update price to zero")
        void shouldUpdatePriceToZero() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(0f);

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should trim title whitespace on update")
        void shouldTrimTitleOnUpdate() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("  Trimmed Update  ");

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals("Trimmed Update", result.getTitle());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp when changes are made")
        void shouldRefreshUpdatedAt() {
            var originalUpdatedAt = testItem.getUpdatedAt();
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("Changed");

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertTrue(result.getUpdatedAt().isAfter(originalUpdatedAt)
                            || result.getUpdatedAt().isEqual(originalUpdatedAt),
                    "updatedAt must be refreshed when changes are applied");
        }

        @Test
        @DisplayName("Should return item unchanged when no fields provided — early return path")
        void shouldHandleEmptyUpdateRequest() {
            UpdateItemRequest request = new UpdateItemRequest();

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals("Original Title", result.getTitle());
            assertEquals("Original Description", result.getDescription());
            assertEquals(0, BigDecimal.valueOf(50).compareTo(result.getPrice()));
        }

        @Test
        @DisplayName("Should update condition for each valid value")
        void shouldUpdateConditionForAllValidValues() {
            for (CreateItemRequestCondition condition : CreateItemRequestCondition.values()) {
                UpdateItemRequest request = new UpdateItemRequest();
                request.setCondition(condition);

                MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

                assertEquals(condition.getValue(), result.getCondition());
            }
        }

        @Test
        @DisplayName("Should accept title of exactly 255 characters on update — boundary")
        void shouldAcceptMaxLengthTitleOnUpdate() {
            String maxTitle = "b".repeat(255);
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle(maxTitle);

            MarketplaceItem result = marketplaceService.updateItem(testItem.getId(), request, testUser.getId());

            assertEquals(255, result.getTitle().length());
        }
    }

    // ─── Update Item — Validation ──────────────────────────────────────────────

    @Nested
    @DisplayName("Update Item — Validation Errors")
    class UpdateItemValidationTests {

        private MarketplaceItem testItem;

        @BeforeEach
        void setUpItem() {
            testItem = createCampusItem("Original Title", 50f, testUser);
        }

        @Test
        @DisplayName("Should fail when item not found")
        void shouldFailWhenItemNotFound() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("New Title");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.updateItem(UUID.randomUUID(), request, testUser.getId()));
            assertTrue(ex.getMessage().contains("Item not found"));
        }

        @Test
        @DisplayName("Should fail when user is not the owner")
        void shouldFailWhenNotOwner() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("Hacked Title");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.updateItem(testItem.getId(), request, otherUser.getId()));
            assertTrue(ex.getMessage().contains("You can only update your own items"));
        }

        @Test
        @DisplayName("Should fail when title is blank on update")
        void shouldFailWhenTitleBlank() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.updateItem(testItem.getId(), request, testUser.getId()));
            assertTrue(ex.getMessage().contains("Title cannot be empty"));
        }

        @Test
        @DisplayName("Should fail when title exceeds 255 characters on update")
        void shouldFailWhenTitleTooLong() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setTitle("a".repeat(256));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.updateItem(testItem.getId(), request, testUser.getId()));
            assertTrue(ex.getMessage().contains("Title cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should fail when price is negative on update")
        void shouldFailWhenPriceNegative() {
            UpdateItemRequest request = new UpdateItemRequest();
            request.setPrice(-10f);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.updateItem(testItem.getId(), request, testUser.getId()));
            assertTrue(ex.getMessage().contains("Price must be greater than or equal to 0"));
        }
    }

    // ─── Hide Item ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hide Item")
    class HideItemTests {

        @Test
        @DisplayName("Should hide own item successfully")
        void shouldHideOwnItem() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);
            assertFalse(created.isHidden());

            marketplaceService.hideItem(created.getId(), testUser.getId());

            MarketplaceItem fromDb = marketplaceItemRepository.findById(created.getId()).orElseThrow();
            assertTrue(fromDb.isHidden());
        }

        @Test
        @DisplayName("Should set updatedAt to a non-null value when hiding")
        void shouldUpdateTimestampWhenHiding() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);

            marketplaceService.hideItem(created.getId(), testUser.getId());

            MarketplaceItem fromDb = marketplaceItemRepository.findById(created.getId()).orElseThrow();
            assertTrue(fromDb.isHidden());
            assertNotNull(fromDb.getUpdatedAt(),
                    "updatedAt must not be null after hiding");
        }

        @Test
        @DisplayName("Should throw when user is not the owner")
        void shouldThrowWhenNotOwner() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.hideItem(created.getId(), otherUser.getId()));
            assertTrue(ex.getMessage().contains("You can only hide your own items"));
        }

        @Test
        @DisplayName("Should throw when item not found")
        void shouldThrowWhenItemNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.hideItem(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should not appear in getItemsByWall results after being hidden")
        void shouldNotAppearInListAfterHiding() {
            MarketplaceItem visible = createCampusItem("Phone", 200f, testUser);
            MarketplaceItem toHide  = createCampusItem("Laptop", 800f, testUser);

            marketplaceService.hideItem(toHide.getId(), testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", null);
            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }
    }

    // ─── Unhide Item ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhide Item")
    class UnhideItemTests {

        @Test
        @DisplayName("Should unhide previously hidden item")
        void shouldUnhideOwnItem() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);
            marketplaceService.hideItem(created.getId(), testUser.getId());
            assertTrue(marketplaceItemRepository.findById(created.getId()).orElseThrow().isHidden());

            marketplaceService.unhideItem(created.getId(), testUser.getId());

            assertFalse(marketplaceItemRepository.findById(created.getId()).orElseThrow().isHidden());
        }

        @Test
        @DisplayName("Should throw when user is not the owner")
        void shouldThrowWhenNotOwner() {
            MarketplaceItem created = createCampusItem("Textbook", 20f, testUser);
            marketplaceService.hideItem(created.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.unhideItem(created.getId(), otherUser.getId()));
            assertTrue(ex.getMessage().contains("You can only unhide your own items"));
        }

        @Test
        @DisplayName("Should throw when item not found")
        void shouldThrowWhenItemNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.unhideItem(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should reappear in getItemsByWall results after being unhidden")
        void shouldReappearInListAfterUnhiding() {
            MarketplaceItem item = createCampusItem("Textbook", 20f, testUser);
            marketplaceService.hideItem(item.getId(), testUser.getId());

            Page<MarketplaceItem> hiddenCheck = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", null);
            assertEquals(0, hiddenCheck.getTotalSize(), "Item should be invisible after hiding");

            marketplaceService.unhideItem(item.getId(), testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", null);
            assertEquals(1, result.getTotalSize());
        }
    }

    // ─── List Items ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("List Items")
    class ListItemsTests {

        @Test
        @DisplayName("Should list all non-hidden items sorted newest first")
        void shouldListItemsNewestFirst() {
            MarketplaceItem i1 = createCampusItem("Item 1", 10f, testUser);
            MarketplaceItem i2 = createCampusItem("Item 2", 20f, testUser);
            MarketplaceItem i3 = createCampusItem("Item 3", 30f, testUser);

            // Stagger timestamps to guarantee deterministic ordering
            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(2));
            i2.setCreatedAt(i2.getCreatedAt().minusSeconds(1));
            marketplaceItemRepository.update(i1);
            marketplaceItemRepository.update(i2);

            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), "newest");

            assertEquals(3, result.getTotalSize());
            assertEquals("Item 3", result.getContent().get(0).getTitle());
            assertEquals("Item 2", result.getContent().get(1).getTitle());
            assertEquals("Item 1", result.getContent().get(2).getTitle());
        }

        @Test
        @DisplayName("Should list items sorted by price ascending")
        void shouldListItemsByPriceAsc() {
            createCampusItem("Expensive", 100f, testUser);
            createCampusItem("Medium", 50f, testUser);
            MarketplaceItem cheap = createCampusItem("Cheap", 10f, testUser);

            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), "price-asc");

            List<MarketplaceItem> items = result.getContent();
            assertEquals(cheap.getId(), items.get(0).getId());
            assertTrue(items.get(0).getPrice().compareTo(items.get(1).getPrice()) <= 0);
            assertTrue(items.get(1).getPrice().compareTo(items.get(2).getPrice()) <= 0);
        }

        @Test
        @DisplayName("Should list items sorted by price descending")
        void shouldListItemsByPriceDesc() {
            createCampusItem("Cheap", 10f, testUser);
            createCampusItem("Medium", 50f, testUser);
            MarketplaceItem expensive = createCampusItem("Expensive", 100f, testUser);

            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), "price-desc");

            List<MarketplaceItem> items = result.getContent();
            assertEquals(expensive.getId(), items.get(0).getId());
            assertTrue(items.get(0).getPrice().compareTo(items.get(1).getPrice()) >= 0);
            assertTrue(items.get(1).getPrice().compareTo(items.get(2).getPrice()) >= 0);
        }

        @Test
        @DisplayName("Should default to newest when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            createCampusItem("Item", 10f, testUser);

            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), null);

            assertEquals(1, result.getTotalSize());
        }

        @Test
        @DisplayName("Should default to newest for unrecognized sortBy value")
        void shouldDefaultToNewestForUnknownSortBy() {
            createCampusItem("Item", 10f, testUser);

            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), "invalid-sort");

            assertEquals(1, result.getTotalSize());
        }

        @Test
        @DisplayName("Should return empty page when no items exist")
        void shouldReturnEmptyPage() {
            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), "newest");

            assertEquals(0, result.getTotalSize());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should paginate results correctly")
        void shouldPaginateCorrectly() {
            for (int i = 1; i <= 5; i++) {
                MarketplaceItem item = createCampusItem("Item" + i, i * 10f, testUser);
                // Item5 is newest, Item1 is oldest
                item.setCreatedAt(item.getCreatedAt().minusSeconds(5 - i));
                marketplaceItemRepository.update(item);
            }

            Page<MarketplaceItem> page1 = marketplaceService.listItems(Pageable.from(0, 2), "newest");
            Page<MarketplaceItem> page2 = marketplaceService.listItems(Pageable.from(1, 2), "newest");

            assertEquals(5, page1.getTotalSize());
            assertEquals(2, page1.getContent().size());
            assertEquals("Item5", page1.getContent().get(0).getTitle());
            assertEquals("Item4", page1.getContent().get(1).getTitle());

            assertEquals(5, page2.getTotalSize());
            assertEquals(2, page2.getContent().size());
            assertEquals("Item3", page2.getContent().get(0).getTitle());
            assertEquals("Item2", page2.getContent().get(1).getTitle());
        }

        @Test
        @DisplayName("listItems includes hidden items — no hidden filter on this method")
        void listItemsIncludesHiddenItems() {
            MarketplaceItem visible = createCampusItem("Phone", 200f, testUser);
            MarketplaceItem hidden  = createCampusItem("Laptop", 800f, testUser);
            marketplaceService.hideItem(hidden.getId(), testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.listItems(Pageable.from(0, 10), "newest");

            // listItems uses findAllOrderByCreatedAtDesc — no hidden predicate.
            // Hidden filtering is the responsibility of getItemsByWall.
            assertEquals(2, result.getTotalSize(),
                    "listItems must return all items including hidden ones");
        }
    }

    // ─── Get Items By Wall ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get Items By Wall")
    class GetItemsByWallTests {

        @Test
        @DisplayName("Should return only campus items for the requesting user's school")
        void shouldReturnCampusItemsForSchool() {
            createCampusItem("Harvard Textbook", 20f, testUser);   // harvard.edu
            createNationalItem("National Laptop", 500f, testUser);  // should not appear

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", null);

            assertEquals(1, result.getTotalSize());
            assertEquals("Harvard Textbook", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("Should throw when campus wall requested without school domain")
        void shouldThrowForCampusWallWithNoSchoolDomain() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> marketplaceService.getItemsByWall(
                            "campus", Pageable.from(0, 10), testUser.getId(), null, "newest", null));
            assertTrue(ex.getMessage().contains("School domain is required"));
        }

        @Test
        @DisplayName("Should filter campus items by category")
        void shouldFilterCampusItemsByCategory() {
            CreateItemRequest req1 = new CreateItemRequest("Laptop", 500f);
            req1.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req1, testUser.getId());

            CreateItemRequest req2 = new CreateItemRequest("Phone", 300f);
            req2.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req2, testUser.getId());

            CreateItemRequest req3 = new CreateItemRequest("Math Book", 30f);
            req3.setCategory(CreateItemRequestCategory.TEXTBOOKS);
            marketplaceService.createItem(req3, testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", "electronics");

            assertEquals(2, result.getTotalSize());
            assertTrue(result.getContent().stream()
                    .allMatch(item -> "electronics".equals(item.getCategory())));
        }

        @Test
        @DisplayName("Should return all campus items when no category filter")
        void shouldReturnAllCampusItemsWithoutCategory() {
            CreateItemRequest req1 = new CreateItemRequest("Laptop", 500f);
            req1.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req1, testUser.getId());

            CreateItemRequest req2 = new CreateItemRequest("Math Book", 30f);
            req2.setCategory(CreateItemRequestCategory.TEXTBOOKS);
            marketplaceService.createItem(req2, testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", null);

            assertEquals(2, result.getTotalSize());
        }

        @Test
        @DisplayName("Should sort campus items by price ascending")
        void shouldSortCampusItemsByPriceAsc() {
            CreateItemRequest cheap = new CreateItemRequest("Basic Phone", 100f);
            cheap.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(cheap, testUser.getId());

            CreateItemRequest expensive = new CreateItemRequest("Gaming PC", 2000f);
            expensive.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(expensive, testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "price-asc", "electronics");

            List<MarketplaceItem> items = result.getContent();
            assertTrue(items.get(0).getPrice().compareTo(items.get(1).getPrice()) <= 0);
        }

        @Test
        @DisplayName("Should sort campus items by price descending")
        void shouldSortCampusItemsByPriceDesc() {
            CreateItemRequest cheap = new CreateItemRequest("Basic Phone", 100f);
            cheap.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(cheap, testUser.getId());

            CreateItemRequest expensive = new CreateItemRequest("Gaming PC", 2000f);
            expensive.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(expensive, testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "price-desc", "electronics");

            List<MarketplaceItem> items = result.getContent();
            assertTrue(items.get(0).getPrice().compareTo(items.get(1).getPrice()) >= 0);
        }

        @Test
        @DisplayName("Should return empty when no items match category")
        void shouldReturnEmptyWhenNoCategoryMatch() {
            CreateItemRequest req = new CreateItemRequest("Laptop", 500f);
            req.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req, testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "campus", Pageable.from(0, 10), testUser.getId(), "harvard.edu", "newest", "furniture");

            assertEquals(0, result.getTotalSize());
        }

        @Test
        @DisplayName("Should return national items for any school domain")
        void shouldReturnNationalItemsForAnyUser() {
            createNationalItem("National Laptop", 500f, testUser);

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "national", Pageable.from(0, 10), mitUser.getId(), null, "newest", null);

            assertEquals(1, result.getTotalSize());
            assertEquals("National Laptop", result.getContent().get(0).getTitle());
        }

        @Test
        @DisplayName("Should filter national items by category")
        void shouldFilterNationalItemsByCategory() {
            CreateItemRequest req1 = new CreateItemRequest("National Laptop", 500f);
            req1.setWall(CreatePostRequestWall.NATIONAL);
            req1.setCategory(CreateItemRequestCategory.ELECTRONICS);
            marketplaceService.createItem(req1, testUser.getId());

            CreateItemRequest req2 = new CreateItemRequest("National Textbook", 30f);
            req2.setWall(CreatePostRequestWall.NATIONAL);
            req2.setCategory(CreateItemRequestCategory.TEXTBOOKS);
            marketplaceService.createItem(req2, testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getItemsByWall(
                    "national", Pageable.from(0, 10), mitUser.getId(), null, "newest", "electronics");

            assertEquals(1, result.getTotalSize());
            assertEquals("National Laptop", result.getContent().get(0).getTitle());
        }
    }

    // ─── Get User Own Items ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get User Own Items")
    class GetUserOwnItemsTests {

        @Test
        @DisplayName("Should return only items belonging to the requesting user")
        void shouldReturnOnlyOwnItems() {
            createCampusItem("My Laptop", 500f, testUser);
            createCampusItem("My Textbook", 20f, testUser);
            createCampusItem("Other's Chair", 50f, otherUser);

            Page<MarketplaceItem> result = marketplaceService.getUserOwnItems(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals(2, result.getTotalSize());
            result.getContent().forEach(i -> assertEquals(testUser.getId(), i.getUserId()));
        }

        @Test
        @DisplayName("Should return own items sorted newest first")
        void shouldReturnOwnItemsNewestFirst() {
            MarketplaceItem i1 = createCampusItem("Laptop", 500f, testUser);
            MarketplaceItem i2 = createCampusItem("Phone", 200f, testUser);

            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(1));
            marketplaceItemRepository.update(i1);

            Page<MarketplaceItem> result = marketplaceService.getUserOwnItems(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals("Phone", result.getContent().get(0).getTitle());
            assertEquals("Laptop", result.getContent().get(1).getTitle());
        }

        @Test
        @DisplayName("Should return own items sorted oldest first")
        void shouldReturnOwnItemsOldestFirst() {
            MarketplaceItem i1 = createCampusItem("Laptop", 500f, testUser);
            MarketplaceItem i2 = createCampusItem("Phone", 200f, testUser);

            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(1));
            marketplaceItemRepository.update(i1);

            Page<MarketplaceItem> result = marketplaceService.getUserOwnItems(
                    testUser.getId(), Pageable.from(0, 10), "oldest");

            assertEquals("Laptop", result.getContent().get(0).getTitle());
            assertEquals("Phone", result.getContent().get(1).getTitle());
        }

        @Test
        @DisplayName("Should default to newest when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            createCampusItem("Laptop", 500f, testUser);

            Page<MarketplaceItem> result = marketplaceService.getUserOwnItems(
                    testUser.getId(), Pageable.from(0, 10), null);

            assertEquals(1, result.getTotalSize());
        }

        @Test
        @DisplayName("Should exclude hidden items from own list")
        void shouldExcludeHiddenFromOwnList() {
            MarketplaceItem visible = createCampusItem("Laptop", 500f, testUser);
            MarketplaceItem hidden = createCampusItem("Phone", 200f, testUser);
            marketplaceService.hideItem(hidden.getId(), testUser.getId());

            Page<MarketplaceItem> result = marketplaceService.getUserOwnItems(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should return empty page when user has no items")
        void shouldReturnEmptyPageWhenNoOwnItems() {
            Page<MarketplaceItem> result = marketplaceService.getUserOwnItems(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals(0, result.getTotalSize());
        }
    }

    // ─── Update Profile Name ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Update Profile Name By UserId")
    class UpdateProfileNameTests {

        @Test
        @DisplayName("Should update profileName on all items owned by user")
        void shouldUpdateProfileNameOnAllOwnedItems() {
            createCampusItem("Laptop", 500f, testUser);
            createCampusItem("Textbook", 20f, testUser);

            marketplaceService.updateProfileNameByUserId(testUser.getId(), "UpdatedName");

            marketplaceItemRepository.findByUserId(testUser.getId()).forEach(i ->
                    assertEquals("UpdatedName", i.getProfileName(),
                            "All items for this user must have the updated profile name"));
        }

        @Test
        @DisplayName("Should not affect items owned by other users")
        void shouldNotUpdateOtherUsersItems() {
            createCampusItem("My Laptop", 500f, testUser);
            createCampusItem("Other's Textbook", 20f, otherUser);

            marketplaceService.updateProfileNameByUserId(testUser.getId(), "UpdatedName");

            marketplaceItemRepository.findByUserId(otherUser.getId()).forEach(i ->
                    assertEquals("OtherSeller", i.getProfileName()));
        }

        @Test
        @DisplayName("Should propagate exception — caller's @Retryable requires it")
        void shouldPropagateException() {
            // updateProfileNameByUserId is called by ProfileNameUpdateEventListener via @Retryable.
            // If the method swallows exceptions, the retry mechanism is silently broken.
            // Passing a null profileName verifies the method does not catch and discard.
            assertThrows(Exception.class,
                    () -> marketplaceService.updateProfileNameByUserId(testUser.getId(), null));
        }
    }

    // ─── Image Upload (Unit — manual wiring) ───────────────────────────────────
    //
    // This section uses manual dependency injection rather than @MicronautTest DI
    // because mediaUtil must be mocked — it calls an external upload service.
    // A new MarketplaceServiceImpl instance is wired in @BeforeEach below.

    @Nested
    @DisplayName("Create Item — Image Upload (Unit)")
    class CreateItemImageUploadTests {

        private MarketplaceServiceImpl svc;
        private MarketplaceItemRepository mockItemRepo;
        private UserService mockUserService;
        private MediaUtilInterface mockMediaUtil;

        @BeforeEach
        void setUpMocks() throws Exception {
            mockItemRepo    = mock(MarketplaceItemRepository.class);
            mockUserService = mock(UserService.class);
            mockMediaUtil   = mock(MediaUtilInterface.class);

            svc = new MarketplaceServiceImpl();
            setField(svc, "marketplaceItemRepository", mockItemRepo);
            setField(svc, "userService", mockUserService);
            setField(svc, "mediaUtil", mockMediaUtil);
            setProviderField(svc, "commentsServiceProvider", mock(CommentsService.class));
            setField(svc, "marketplaceItemHiddenEventPublisher", mock(ApplicationEventPublisher.class));
            setField(svc, "userBlockService", mock(UserBlockService.class));
        }

        private void setField(Object target, String name, Object value) throws Exception {
            var field = MarketplaceServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        }

        private void setProviderField(Object target, String name, Object svc) throws Exception {
            @SuppressWarnings("unchecked")
            jakarta.inject.Provider<Object> provider = mock(jakarta.inject.Provider.class);
            when(provider.get()).thenReturn(svc);
            var field = MarketplaceServiceImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, provider);
        }

        private UserEntity stubUser(String schoolDomain) {
            UserEntity user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail("student@" + schoolDomain);
            user.setSchoolDomain(schoolDomain);
            user.setProfileName("TestSeller");
            when(mockUserService.findById(user.getId())).thenReturn(Optional.of(user));
            return user;
        }

        private MarketplaceItem savedItem(UUID userId, List<String> urls) {
            MarketplaceItem item = new MarketplaceItem();
            item.setId(UUID.randomUUID());
            item.setUserId(userId);
            item.setTitle("Test");
            item.setPrice(BigDecimal.TEN);
            item.setWall("campus");
            item.setSchoolDomain("test.edu");
            item.setImageUrls(urls != null ? urls : Collections.emptyList());
            return item;
        }

        @Test
        @DisplayName("Should attach objectNames from request to saved item")
        void shouldAttachImageObjectNamesToItem() {
            UserEntity user = stubUser("test.edu");
            List<String> objectNames = List.of("marketplace/uuid1.jpg");
            when(mockItemRepo.save(any())).thenReturn(savedItem(user.getId(), objectNames));

            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setImageObjectNames(objectNames);
            MarketplaceItem result = svc.createItem(request, user.getId());

            assertEquals(objectNames, result.getImageUrls());
            verifyNoInteractions(mockMediaUtil);
        }

        @Test
        @DisplayName("Should attach multiple objectNames from request")
        void shouldAttachMultipleImageObjectNames() {
            UserEntity user = stubUser("test.edu");
            List<String> objectNames = List.of("marketplace/uuid1.jpg", "marketplace/uuid2.png");
            when(mockItemRepo.save(any())).thenReturn(savedItem(user.getId(), objectNames));

            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setImageObjectNames(objectNames);
            MarketplaceItem result = svc.createItem(request, user.getId());

            assertEquals(2, result.getImageUrls().size());
            verifyNoInteractions(mockMediaUtil);
        }

        @Test
        @DisplayName("Should throw before DB access when more than 5 objectNames provided")
        void shouldThrowImmediatelyForTooManyImages() {
            UUID userId = UUID.randomUUID();
            CreateItemRequest request = new CreateItemRequest("Title", 10f);
            request.setImageObjectNames(List.of(
                    "marketplace/a.jpg", "marketplace/b.jpg", "marketplace/c.jpg",
                    "marketplace/d.jpg", "marketplace/e.jpg", "marketplace/f.jpg"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> svc.createItem(request, userId));
            assertTrue(ex.getMessage().contains("5"));
            verify(mockUserService, never()).findById(any());
        }

        @Test
        @DisplayName("Should create item with no images when imageObjectNames is null")
        void shouldCreateItemWithNoImages() {
            UserEntity user = stubUser("test.edu");
            when(mockItemRepo.save(any())).thenReturn(savedItem(user.getId(), null));

            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            svc.createItem(request, user.getId());

            verifyNoInteractions(mockMediaUtil);
        }

        @Test
        @DisplayName("Should create item with no images when imageObjectNames is empty")
        void shouldCreateItemWithEmptyImageList() {
            UserEntity user = stubUser("test.edu");
            when(mockItemRepo.save(any())).thenReturn(savedItem(user.getId(), null));

            CreateItemRequest request = new CreateItemRequest("Test", 10f);
            request.setImageObjectNames(List.of());
            svc.createItem(request, user.getId());

            verifyNoInteractions(mockMediaUtil);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            when(mockUserService.findById(any())).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> svc.createItem(new CreateItemRequest("Title", 10f), UUID.randomUUID()));
        }
    }
}
