package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AdminServiceImpl Tests")
class AdminServiceImplTest {

    private AdminServiceImpl adminService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adminService = new AdminServiceImpl();
        
        // Use reflection to inject mock
        try {
            var repoField = AdminServiceImpl.class.getDeclaredField("userRepository");
            repoField.setAccessible(true);
            repoField.set(adminService, userRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {
        
        @Test
        @DisplayName("Should get all users with pagination")
        void shouldGetAllUsersWithPagination() {
            // Given
            Pageable pageable = Pageable.from(0, 20);
            Page<UserEntity> mockPage = mock(Page.class);
            when(userRepository.findAll(pageable)).thenReturn(mockPage);
            
            // When
            Page<UserEntity> result = adminService.getAllUsers(pageable);
            
            // Then
            assertNotNull(result);
            verify(userRepository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {
        
        @Test
        @DisplayName("Should get user by ID")
        void shouldGetUserById() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When
            Optional<UserEntity> result = adminService.getUserById(userId);
            
            // Then
            assertTrue(result.isPresent());
            assertEquals(userId, result.get().getId());
            verify(userRepository).findById(userId);
        }
        
        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            
            // When
            Optional<UserEntity> result = adminService.getUserById(userId);
            
            // Then
            assertFalse(result.isPresent());
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    @DisplayName("Block User Tests")
    class BlockUserTests {
        
        @Test
        @DisplayName("Should block active user successfully")
        void shouldBlockActiveUserSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setAccountStatus("active");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When
            adminService.blockUser(userId);
            
            // Then
            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).update(userCaptor.capture());
            assertEquals("blocked", userCaptor.getValue().getAccountStatus());
        }
        
        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> adminService.blockUser(userId));
            verify(userRepository, never()).update(any());
        }
        
        @Test
        @DisplayName("Should throw exception when user already blocked")
        void shouldThrowExceptionWhenUserAlreadyBlocked() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setAccountStatus("blocked");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> adminService.blockUser(userId));
            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Unblock User Tests")
    class UnblockUserTests {
        
        @Test
        @DisplayName("Should unblock blocked user successfully")
        void shouldUnblockBlockedUserSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setAccountStatus("blocked");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When
            adminService.unblockUser(userId);
            
            // Then
            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).update(userCaptor.capture());
            assertEquals("active", userCaptor.getValue().getAccountStatus());
        }
        
        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> adminService.unblockUser(userId));
            verify(userRepository, never()).update(any());
        }
        
        @Test
        @DisplayName("Should throw exception when user not blocked")
        void shouldThrowExceptionWhenUserNotBlocked() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setAccountStatus("active");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> adminService.unblockUser(userId));
            verify(userRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {
        
        @Test
        @DisplayName("Should soft delete user successfully")
        void shouldSoftDeleteUserSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setAccountStatus("active");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When
            adminService.deleteUser(userId);
            
            // Then
            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).update(userCaptor.capture());
            assertEquals("deleted", userCaptor.getValue().getAccountStatus());
        }
        
        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> adminService.deleteUser(userId));
            verify(userRepository, never()).update(any());
        }
        
        @Test
        @DisplayName("Should throw exception when user already deleted")
        void shouldThrowExceptionWhenUserAlreadyDeleted() {
            // Given
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId);
            user.setAccountStatus("deleted");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> adminService.deleteUser(userId));
            verify(userRepository, never()).update(any());
        }
    }

    // Helper method to create test user
    private UserEntity createTestUser(UUID id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("test@example.edu");
        user.setProfileName("Test User");
        user.setSchoolDomain("example.edu");
        user.setRole("user");
        user.setAccountStatus("active");
        user.setVerified(true);
        user.setPasswordSet(true);
        user.setCreatedAt(ZonedDateTime.now());
        return user;
    }
}
