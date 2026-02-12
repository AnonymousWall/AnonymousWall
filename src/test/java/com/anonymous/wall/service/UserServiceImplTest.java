package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    private UserServiceImpl userService;
    private UserRepository userRepository;
    private ApplicationEventPublisher<ProfileNameChangedEvent> eventPublisher;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        userService = new UserServiceImpl();
        // Use reflection to inject mocks
        try {
            var userRepoField = UserServiceImpl.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(userService, userRepository);
            
            var eventPublisherField = UserServiceImpl.class.getDeclaredField("eventPublisher");
            eventPublisherField.setAccessible(true);
            eventPublisherField.set(userService, eventPublisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Positive: Should find user by valid ID")
        void shouldFindUserByValidId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // Act
            Optional<UserEntity> result = userService.findById(userId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(userId, result.get().getId());
            assertEquals("test@harvard.edu", result.get().getEmail());
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent user ID")
        void shouldReturnEmptyForNonExistentId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act
            Optional<UserEntity> result = userService.findById(userId);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository, times(1)).findById(userId);
        }
    }

    @Nested
    @DisplayName("Find By Email Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Positive: Should find user by valid email")
        void shouldFindUserByValidEmail() {
            // Arrange
            String email = "test@harvard.edu";
            UserEntity user = createTestUser(UUID.randomUUID(), email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // Act
            Optional<UserEntity> result = userService.findByEmail(email);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(email, result.get().getEmail());
            verify(userRepository, times(1)).findByEmail(email);
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent email")
        void shouldReturnEmptyForNonExistentEmail() {
            // Arrange
            String email = "nonexistent@harvard.edu";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act
            Optional<UserEntity> result = userService.findByEmail(email);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository, times(1)).findByEmail(email);
        }
    }

    @Nested
    @DisplayName("Update Profile Name Tests")
    class UpdateProfileNameTests {

        @Test
        @DisplayName("Positive: Should update profile name successfully")
        void shouldUpdateProfileNameSuccessfully() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            user.setProfileName("Old Name");
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity updatedUser = invocation.getArgument(0);
                return updatedUser;
            });

            // Act
            UserEntity result = userService.updateProfileName(userId, "New Name");

            // Assert
            assertNotNull(result);
            assertEquals("New Name", result.getProfileName());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).update(any(UserEntity.class));
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should trim whitespace from profile name")
        void shouldTrimWhitespaceFromProfileName() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity updatedUser = invocation.getArgument(0);
                return updatedUser;
            });

            // Act
            UserEntity result = userService.updateProfileName(userId, "  Spaced Name  ");

            // Assert
            assertEquals("Spaced Name", result.getProfileName());
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should set 'Anonymous' for null profile name")
        void shouldSetAnonymousForNullProfileName() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity updatedUser = invocation.getArgument(0);
                return updatedUser;
            });

            // Act
            UserEntity result = userService.updateProfileName(userId, null);

            // Assert
            assertEquals("Anonymous", result.getProfileName());
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should set 'Anonymous' for empty profile name")
        void shouldSetAnonymousForEmptyProfileName() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(invocation -> {
                UserEntity updatedUser = invocation.getArgument(0);
                return updatedUser;
            });

            // Act
            UserEntity result = userService.updateProfileName(userId, "   ");

            // Assert
            assertEquals("Anonymous", result.getProfileName());
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
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
                () -> userService.updateProfileName(userId, "New Name")
            );
            assertEquals("User not found", exception.getMessage());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, never()).update(any(UserEntity.class));
            verify(eventPublisher, never()).publishEvent(any(ProfileNameChangedEvent.class));
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Positive: Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            user.setProfileName("Updated Name");
            
            when(userRepository.update(user)).thenReturn(user);

            // Act
            UserEntity result = userService.update(user);

            // Assert
            assertNotNull(result);
            assertEquals(userId, result.getId());
            assertEquals("Updated Name", result.getProfileName());
            verify(userRepository, times(1)).update(user);
        }
    }

    @Nested
    @DisplayName("Save User Tests")
    class SaveUserTests {

        @Test
        @DisplayName("Positive: Should save new user successfully")
        void shouldSaveNewUserSuccessfully() {
            // Arrange
            UserEntity user = new UserEntity();
            user.setEmail("newuser@harvard.edu");
            user.setSchoolDomain("harvard.edu");
            user.setVerified(true);
            
            UserEntity savedUser = createTestUser(UUID.randomUUID(), "newuser@harvard.edu");
            when(userRepository.save(user)).thenReturn(savedUser);

            // Act
            UserEntity result = userService.save(user);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("newuser@harvard.edu", result.getEmail());
            verify(userRepository, times(1)).save(user);
        }
    }

    // Helper method to create test users
    private UserEntity createTestUser(UUID id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setSchoolDomain("harvard.edu");
        user.setProfileName("Anonymous");
        user.setVerified(true);
        user.setPasswordSet(false);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}
