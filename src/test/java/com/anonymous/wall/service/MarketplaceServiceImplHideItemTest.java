package com.anonymous.wall.service;

import com.anonymous.wall.entity.MarketplaceItem;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateItemRequest;
import com.anonymous.wall.repository.MarketplaceItemRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.MarketplaceService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("MarketplaceServiceImpl - Hide/Unhide Item Tests")
class MarketplaceServiceImplHideItemTest {

    @Inject
    private MarketplaceService marketplaceService;

    @Inject
    private MarketplaceItemRepository marketplaceItemRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        marketplaceItemRepository.deleteAll();

        testUser = new UserEntity();
        testUser.setEmail("testuser" + System.currentTimeMillis() + "@test.edu");
        testUser.setSchoolDomain("test.edu");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("otheruser" + System.currentTimeMillis() + "@test.edu");
        otherUser.setSchoolDomain("test.edu");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser = userRepository.save(otherUser);
    }

    @AfterEach
    void tearDown() {
        marketplaceItemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Hide Item Tests")
    class HideItemTests {

        @Test
        @DisplayName("Should hide item successfully")
        void shouldHideItemSuccessfully() {
            CreateItemRequest request = new CreateItemRequest("Old Textbook", 20f);
            MarketplaceItem created = marketplaceService.createItem(request, null, testUser.getId());
            assertFalse(created.isHidden());

            marketplaceService.hideItem(created.getId(), testUser.getId());

            MarketplaceItem updated = marketplaceItemRepository.findById(created.getId()).orElseThrow();
            assertTrue(updated.isHidden());
        }

        @Test
        @DisplayName("Should fail to hide when not owner")
        void shouldFailToHideWhenNotOwner() {
            CreateItemRequest request = new CreateItemRequest("Old Textbook", 20f);
            MarketplaceItem created = marketplaceService.createItem(request, null, testUser.getId());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.hideItem(created.getId(), otherUser.getId())
            );
            assertTrue(exception.getMessage().contains("You can only hide your own items"));
        }

        @Test
        @DisplayName("Should fail to hide when item not found")
        void shouldFailToHideWhenItemNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.hideItem(nonExistentId, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("Unhide Item Tests")
    class UnhideItemTests {

        @Test
        @DisplayName("Should unhide item successfully")
        void shouldUnhideItemSuccessfully() {
            CreateItemRequest request = new CreateItemRequest("Old Textbook", 20f);
            MarketplaceItem created = marketplaceService.createItem(request, null, testUser.getId());
            marketplaceService.hideItem(created.getId(), testUser.getId());

            MarketplaceItem hidden = marketplaceItemRepository.findById(created.getId()).orElseThrow();
            assertTrue(hidden.isHidden());

            marketplaceService.unhideItem(created.getId(), testUser.getId());

            MarketplaceItem unhidden = marketplaceItemRepository.findById(created.getId()).orElseThrow();
            assertFalse(unhidden.isHidden());
        }

        @Test
        @DisplayName("Should fail to unhide when not owner")
        void shouldFailToUnhideWhenNotOwner() {
            CreateItemRequest request = new CreateItemRequest("Old Textbook", 20f);
            MarketplaceItem created = marketplaceService.createItem(request, null, testUser.getId());
            marketplaceService.hideItem(created.getId(), testUser.getId());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.unhideItem(created.getId(), otherUser.getId())
            );
            assertTrue(exception.getMessage().contains("You can only unhide your own items"));
        }

        @Test
        @DisplayName("Should fail to unhide when item not found")
        void shouldFailToUnhideWhenItemNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketplaceService.unhideItem(nonExistentId, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }
}
