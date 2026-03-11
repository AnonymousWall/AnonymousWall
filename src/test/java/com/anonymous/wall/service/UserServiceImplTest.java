package com.anonymous.wall.service;

import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.impl.UserServiceImpl;
import io.micronaut.context.event.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    @SuppressWarnings("unchecked")
    void setUp() {
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        userService = new UserServiceImpl();
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

    // ================= findById =================

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Positive: Should find user by valid ID")
        void shouldFindUserByValidId() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            Optional<UserEntity> result = userService.findById(userId);

            assertTrue(result.isPresent());
            assertEquals(userId, result.get().getId());
            assertEquals("test@harvard.edu", result.get().getEmail());
            verify(userRepository, times(1)).findById(userId);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent user ID")
        void shouldReturnEmptyForNonExistentId() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            Optional<UserEntity> result = userService.findById(userId);

            assertTrue(result.isEmpty());
            verify(userRepository, times(1)).findById(userId);
        }
    }

    // ================= findByEmail =================

    @Nested
    @DisplayName("findByEmail Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Positive: Should find user by valid email")
        void shouldFindUserByValidEmail() {
            String email = "test@harvard.edu";
            UserEntity user = createTestUser(UUID.randomUUID(), email);
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            Optional<UserEntity> result = userService.findByEmail(email);

            assertTrue(result.isPresent());
            assertEquals(email, result.get().getEmail());
            verify(userRepository, times(1)).findByEmail(email);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent email")
        void shouldReturnEmptyForNonExistentEmail() {
            String email = "nonexistent@harvard.edu";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            Optional<UserEntity> result = userService.findByEmail(email);

            assertTrue(result.isEmpty());
            verify(userRepository, times(1)).findByEmail(email);
        }
    }

    // ================= updateProfileName =================

    @Nested
    @DisplayName("updateProfileName Tests")
    class UpdateProfileNameTests {

        @Test
        @DisplayName("Positive: Should update profile name successfully")
        void shouldUpdateProfileNameSuccessfully() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            user.setProfileName("Old Name");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            UserEntity result = userService.updateProfileName(userId, "New Name");

            assertNotNull(result);
            assertEquals("New Name", result.getProfileName());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, times(1)).update(any(UserEntity.class));
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should publish event with correct old and new profile names")
        void shouldPublishEventWithCorrectNames() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            user.setProfileName("Old Name");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateProfileName(userId, "New Name");

            ArgumentCaptor<ProfileNameChangedEvent> captor = ArgumentCaptor.forClass(ProfileNameChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            ProfileNameChangedEvent event = captor.getValue();
            assertEquals(userId, event.getUserId());
            assertEquals("Old Name", event.getOldName());
            assertEquals("New Name", event.getNewName());
        }

        @Test
        @DisplayName("Positive: Should trim whitespace from profile name")
        void shouldTrimWhitespaceFromProfileName() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            UserEntity result = userService.updateProfileName(userId, "  Spaced Name  ");

            assertEquals("Spaced Name", result.getProfileName());
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should set 'Anonymous' for null profile name")
        void shouldSetAnonymousForNullProfileName() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            UserEntity result = userService.updateProfileName(userId, null);

            assertEquals("Anonymous", result.getProfileName());
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should set 'Anonymous' for blank profile name")
        void shouldSetAnonymousForBlankProfileName() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            UserEntity result = userService.updateProfileName(userId, "   ");

            assertEquals("Anonymous", result.getProfileName());
            verify(eventPublisher, times(1)).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Positive: Should publish event with 'Anonymous' as new name when blank name given")
        void shouldPublishEventWithAnonymousWhenBlankName() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            user.setProfileName("Some Name");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.update(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));

            userService.updateProfileName(userId, "");

            ArgumentCaptor<ProfileNameChangedEvent> captor = ArgumentCaptor.forClass(ProfileNameChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertEquals("Anonymous", captor.getValue().getNewName());
            assertEquals("Some Name", captor.getValue().getOldName());
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.updateProfileName(userId, "New Name")
            );

            assertEquals("User not found", exception.getMessage());
            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, never()).update(any(UserEntity.class));
            verify(eventPublisher, never()).publishEvent(any(ProfileNameChangedEvent.class));
        }

        @Test
        @DisplayName("Edge: Returned entity is the one returned by the repository update call")
        void shouldReturnEntityFromRepository() {
            UUID userId = UUID.randomUUID();
            UserEntity stored = createTestUser(userId, "test@harvard.edu");
            UserEntity repoResponse = createTestUser(userId, "test@harvard.edu");
            repoResponse.setProfileName("Repo Name");

            when(userRepository.findById(userId)).thenReturn(Optional.of(stored));
            when(userRepository.update(any(UserEntity.class))).thenReturn(repoResponse);

            UserEntity result = userService.updateProfileName(userId, "Repo Name");

            assertSame(repoResponse, result);
        }
    }

    // ================= update =================

    @Nested
    @DisplayName("update Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Positive: Should delegate update to repository and return result")
        void shouldUpdateUserSuccessfully() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            user.setProfileName("Updated Name");

            when(userRepository.update(user)).thenReturn(user);

            UserEntity result = userService.update(user);

            assertNotNull(result);
            assertSame(user, result);
            assertEquals("Updated Name", result.getProfileName());
            verify(userRepository, times(1)).update(user);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Positive: Should pass the exact same entity instance to the repository")
        void shouldPassExactEntityToRepository() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "test@harvard.edu");
            when(userRepository.update(user)).thenReturn(user);

            userService.update(user);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).update(captor.capture());
            assertSame(user, captor.getValue());
        }
    }

    // ================= save =================

    @Nested
    @DisplayName("save Tests")
    class SaveUserTests {

        @Test
        @DisplayName("Positive: Should save new user and return persisted entity")
        void shouldSaveNewUserSuccessfully() {
            UserEntity user = new UserEntity();
            user.setEmail("newuser@harvard.edu");
            user.setSchoolDomain("harvard.edu");
            user.setVerified(true);

            UserEntity savedUser = createTestUser(UUID.randomUUID(), "newuser@harvard.edu");
            when(userRepository.save(user)).thenReturn(savedUser);

            UserEntity result = userService.save(user);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("newuser@harvard.edu", result.getEmail());
            verify(userRepository, times(1)).save(user);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        @DisplayName("Positive: Should pass the exact same entity instance to the repository")
        void shouldPassExactEntityToRepository() {
            UserEntity user = new UserEntity();
            user.setEmail("newuser@harvard.edu");
            when(userRepository.save(user)).thenReturn(user);

            userService.save(user);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertSame(user, captor.getValue());
        }
    }

    // ================= existsById =================

    @Nested
    @DisplayName("existsById Tests")
    class ExistsByIdTests {

        @Test
        @DisplayName("Positive: Should return true when user exists")
        void shouldReturnTrueWhenUserExists() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(true);

            assertTrue(userService.existsById(userId));
            verify(userRepository, times(1)).existsById(userId);
        }

        @Test
        @DisplayName("Negative: Should return false when user does not exist")
        void shouldReturnFalseWhenUserDoesNotExist() {
            UUID userId = UUID.randomUUID();
            when(userRepository.existsById(userId)).thenReturn(false);

            assertFalse(userService.existsById(userId));
            verify(userRepository, times(1)).existsById(userId);
        }
    }

    // ================= isUserBlocked =================

    @Nested
    @DisplayName("isUserBlocked Tests")
    class IsUserBlockedTests {

        @Test
        @DisplayName("Positive: Should return true when user is blocked")
        void shouldReturnTrueWhenUserIsBlocked() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "blocked@harvard.edu");
            user.setBlocked(true);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertTrue(userService.isUserBlocked(userId));
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("Negative: Should return false when user is not blocked")
        void shouldReturnFalseWhenUserIsNotBlocked() {
            UUID userId = UUID.randomUUID();
            UserEntity user = createTestUser(userId, "active@harvard.edu");
            user.setBlocked(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            assertFalse(userService.isUserBlocked(userId));
            verify(userRepository, times(1)).findById(userId);
        }

        @Test
        @DisplayName("Edge: Should return false when user does not exist")
        void shouldReturnFalseWhenUserDoesNotExist() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertFalse(userService.isUserBlocked(userId));
            verify(userRepository, times(1)).findById(userId);
        }
    }

    // ================= Helper =================

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