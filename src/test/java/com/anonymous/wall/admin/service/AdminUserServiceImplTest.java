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

@DisplayName("AdminUserServiceImpl Tests")
class AdminUserServiceImplTest {

    private AdminUserServiceImpl service;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AdminUserServiceImpl();
        
        // Inject mock via reflection
        try {
            var repoField = AdminUserServiceImpl.class.getDeclaredField("userRepository");
            repoField.setAccessible(true);
            repoField.set(service, userRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repository", e);
        }
    }

    private UserEntity createMockUser(UUID id, String email, String schoolDomain, boolean blocked) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setSchoolDomain(schoolDomain);
        user.setBlocked(blocked);
        user.setReportCount(0);
        user.setCreatedAt(OffsetDateTime.now());
        user.setProfileName("Test User");
        return user;
    }

    @Nested
    @DisplayName("GetAllUsers Tests - No Filters")
    class GetAllUsersNoFiltersTests {

        @Test
        @DisplayName("Positive: Should return all users with default pagination")
        void shouldReturnAllUsersWithDefaultPagination() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, null, null);

            // Assert
            assertNotNull(result);
            verify(userRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by createdAt descending")
        void shouldSortByCreatedAtDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by createdAt ascending")
        void shouldSortByCreatedAtAscending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtAsc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findAllOrderByCreatedAtAsc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by schoolDomain descending")
        void shouldSortBySchoolDomainDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderBySchoolDomainDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "schoolDomain", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findAllOrderBySchoolDomainDesc(pageable);
        }

        @Test
        @DisplayName("Positive: Should sort by reportCount descending")
        void shouldSortByReportCountDescending() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByReportCountDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "reportCount", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findAllOrderByReportCountDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should be case-insensitive for sortBy")
        void shouldBeCaseInsensitiveForSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "CREATEDAT", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Edge: Should default to desc when sortOrder is null")
        void shouldDefaultToDescWhenSortOrderIsNull() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAllOrderByCreatedAtDesc(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "createdAt", null);

            // Assert
            assertNotNull(result);
            verify(userRepository).findAllOrderByCreatedAtDesc(pageable);
        }

        @Test
        @DisplayName("Negative: Should fallback to findAll for invalid sortBy")
        void shouldFallbackToFindAllForInvalidSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAll(pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, null, "invalid", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("GetAllUsers Tests - With Blocked Filter")
    class GetAllUsersWithBlockedFilterTests {

        @Test
        @DisplayName("Positive: Should filter blocked users with createdAt desc")
        void shouldFilterBlockedUsersWithCreatedAtDesc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findByBlockedOrderByCreatedAtDesc(true, pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, true, "createdAt", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findByBlockedOrderByCreatedAtDesc(true, pageable);
        }

        @Test
        @DisplayName("Positive: Should filter non-blocked users with createdAt asc")
        void shouldFilterNonBlockedUsersWithCreatedAtAsc() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findByBlockedOrderByCreatedAtAsc(false, pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, false, "createdAt", "asc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findByBlockedOrderByCreatedAtAsc(false, pageable);
        }

        @Test
        @DisplayName("Edge: Should filter blocked users with default sorting")
        void shouldFilterBlockedUsersWithDefaultSorting() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findByBlockedOrderByCreatedAtDesc(true, pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, true, null, null);

            // Assert
            assertNotNull(result);
            verify(userRepository).findByBlockedOrderByCreatedAtDesc(true, pageable);
        }

        @Test
        @DisplayName("Edge: Should warn and use findByBlocked for unsupported sortBy")
        void shouldWarnAndUseFindByBlockedForUnsupportedSortBy() {
            // Arrange
            Pageable pageable = Pageable.from(0, 10);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findByBlocked(true, pageable)).thenReturn(mockPage);

            // Act
            Page<UserEntity> result = service.getAllUsers(pageable, true, "reportCount", "desc");

            // Assert
            assertNotNull(result);
            verify(userRepository).findByBlocked(true, pageable);
        }
    }

    @Nested
    @DisplayName("GetUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Positive: Should return user for valid ID")
        void shouldReturnUserForValidId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createMockUser(userId, "test@harvard.edu", "harvard.edu", false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // Act
            UserEntity result = service.getUserById(userId);

            // Assert
            assertNotNull(result);
            assertEquals(userId, result.getId());
            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getUserById(userId)
            );
            assertTrue(exception.getMessage().contains("User not found"));
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("BlockUser Tests")
    class BlockUserTests {

        @Test
        @DisplayName("Positive: Should block user by setting blocked to true")
        void shouldBlockUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createMockUser(userId, "test@mit.edu", "mit.edu", false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            service.blockUser(userId);

            // Assert
            assertTrue(user.isBlocked());
            verify(userRepository).findById(userId);
            verify(userRepository).update(argThat(u -> u.isBlocked()));
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.blockUser(userId)
            );
            assertTrue(exception.getMessage().contains("User not found"));
            verify(userRepository).findById(userId);
            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("Edge: Should handle already blocked user")
        void shouldHandleAlreadyBlockedUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createMockUser(userId, "test@stanford.edu", "stanford.edu", true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            service.blockUser(userId);

            // Assert
            assertTrue(user.isBlocked());
            verify(userRepository).update(user);
        }
    }

    @Nested
    @DisplayName("UnblockUser Tests")
    class UnblockUserTests {

        @Test
        @DisplayName("Positive: Should unblock user by setting blocked to false")
        void shouldUnblockUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createMockUser(userId, "test@yale.edu", "yale.edu", true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            service.unblockUser(userId);

            // Assert
            assertFalse(user.isBlocked());
            verify(userRepository).findById(userId);
            verify(userRepository).update(argThat(u -> !u.isBlocked()));
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.unblockUser(userId)
            );
            assertTrue(exception.getMessage().contains("User not found"));
            verify(userRepository).findById(userId);
            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("Edge: Should handle already unblocked user")
        void shouldHandleAlreadyUnblockedUser() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createMockUser(userId, "test@princeton.edu", "princeton.edu", false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenReturn(user);

            // Act
            service.unblockUser(userId);

            // Assert
            assertFalse(user.isBlocked());
            verify(userRepository).update(user);
        }
    }
}
