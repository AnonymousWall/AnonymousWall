package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AdminUserService Tests")
class AdminUserServiceTest {

    private AdminUserServiceImpl adminUserService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adminUserService = new AdminUserServiceImpl();
        
        try {
            var repoField = AdminUserServiceImpl.class.getDeclaredField("userRepository");
            repoField.setAccessible(true);
            repoField.set(adminUserService, userRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Get All Users - Positive Cases")
    class GetAllUsersPositiveCases {

        @Test
        @DisplayName("Should get all users without filters")
        void shouldGetAllUsersWithoutFilters() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, null, null);

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should sort by createdAt descending")
        void shouldSortByCreatedAtDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by createdAt ascending")
        void shouldSortByCreatedAtAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Should sort by schoolDomain descending")
        void shouldSortBySchoolDomainDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderBySchoolDomainDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "schoolDomain", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderBySchoolDomainDesc(pageable);
        }

        @Test
        @DisplayName("Should sort by reportCount descending")
        void shouldSortByReportCountDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByReportCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "reportCount", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderByReportCountDesc(pageable);
        }

        @Test
        @DisplayName("Should filter blocked users")
        void shouldFilterBlockedUsers() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findByBlockedOrderByCreatedAtDesc(true, pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, true, null, "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findByBlockedOrderByCreatedAtDesc(true, pageable);
        }

        @Test
        @DisplayName("Should filter non-blocked users")
        void shouldFilterNonBlockedUsers() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findByBlockedOrderByCreatedAtAsc(false, pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, false, null, "asc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findByBlockedOrderByCreatedAtAsc(false, pageable);
        }

        @Test
        @DisplayName("Should handle case-insensitive sortBy")
        void shouldHandleCaseInsensitiveSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "CREATEDAT", "DESC");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }
    }

    @Nested
    @DisplayName("Get All Users - Negative Cases")
    class GetAllUsersNegativeCases {

        @Test
        @DisplayName("Should use default sorting for unknown sortBy")
        void shouldUseDefaultForUnknownSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "invalid", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should default to desc when sortOrder is null")
        void shouldDefaultToDescWhenSortOrderNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "createdAt", null);

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }
    }

    @Nested
    @DisplayName("Get All Users - Edge Cases")
    class GetAllUsersEdgeCases {

        @Test
        @DisplayName("Should handle empty sortOrder string")
        void shouldHandleEmptySortOrder() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, "createdAt", "");

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Should handle different page sizes")
        void shouldHandleDifferentPageSizes() {
            // Arrange
            Pageable pageable = Pageable.from(0, 100);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = adminUserService.getAllUsers(pageable, null, null, null);

            // Assert
            assertNotNull(result);
            verify(userRepository, times(1)).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Get User By ID - Positive Cases")
    class GetUserByIdPositiveCases {

        @Test
        @DisplayName("Should get user by ID successfully")
        void shouldGetUserById() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // Act
            UserEntity result = adminUserService.getUserById(userId);

            // Assert
            assertNotNull(result);
            assertEquals(userId, result.getId());
            verify(userRepository, times(1)).findById(userId);
        }
    }

    @Nested
    @DisplayName("Get User By ID - Negative Cases")
    class GetUserByIdNegativeCases {

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowForNonExistentUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.getUserById(userId));
            assertTrue(exception.getMessage().contains("User not found"));
            verify(userRepository, times(1)).findById(userId);
        }
    }

    @Nested
    @DisplayName("Block User - Positive Cases")
    class BlockUserPositiveCases {

        @Test
        @DisplayName("Should block user successfully")
        void shouldBlockUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setBlocked(false);
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminUserService.blockUser(userId);

            // Assert
            assertTrue(user.isBlocked());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).update(user);
        }

        @Test
        @DisplayName("Should block already blocked user without error")
        void shouldBlockAlreadyBlockedUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setBlocked(true);
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminUserService.blockUser(userId);

            // Assert
            assertTrue(user.isBlocked());
            verify(userRepository, times(1)).update(user);
        }
    }

    @Nested
    @DisplayName("Block User - Negative Cases")
    class BlockUserNegativeCases {

        @Test
        @DisplayName("Should throw exception when blocking non-existent user")
        void shouldThrowWhenBlockingNonExistentUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> adminUserService.blockUser(userId));
            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Unblock User - Positive Cases")
    class UnblockUserPositiveCases {

        @Test
        @DisplayName("Should unblock user successfully")
        void shouldUnblockUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setBlocked(true);
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminUserService.unblockUser(userId);

            // Assert
            assertFalse(user.isBlocked());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).update(user);
        }

        @Test
        @DisplayName("Should unblock already unblocked user without error")
        void shouldUnblockAlreadyUnblockedUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setBlocked(false);
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminUserService.unblockUser(userId);

            // Assert
            assertFalse(user.isBlocked());
            verify(userRepository, times(1)).update(user);
        }
    }

    @Nested
    @DisplayName("Unblock User - Negative Cases")
    class UnblockUserNegativeCases {

        @Test
        @DisplayName("Should throw exception when unblocking non-existent user")
        void shouldThrowWhenUnblockingNonExistentUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> adminUserService.unblockUser(userId));
            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle multiple block/unblock operations")
        void shouldHandleMultipleBlockUnblock() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setBlocked(false);
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            adminUserService.blockUser(userId);
            assertTrue(user.isBlocked());
            
            adminUserService.unblockUser(userId);
            assertFalse(user.isBlocked());
            
            adminUserService.blockUser(userId);
            assertTrue(user.isBlocked());

            // Assert
            verify(userRepository, times(3)).findById(userId);
            verify(userRepository, times(3)).update(user);
        }
    }

    private UserEntity createTestUser(UUID userId) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("test@harvard.edu");
        user.setSchoolDomain("harvard.edu");
        user.setBlocked(false);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}
