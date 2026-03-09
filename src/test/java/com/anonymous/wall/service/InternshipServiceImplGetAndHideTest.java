package com.anonymous.wall.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.repository.InternshipRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.InternshipService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("InternshipServiceImpl - Get, Hide, Unhide Tests")
class InternshipServiceImplGetAndHideTest {

    @Inject
    private InternshipService internshipService;

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;
    private UserEntity otherUser;

    @BeforeEach
    void setUp() {
        // Clean up
        internshipRepository.deleteAll();

        // Create test users
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
        internshipRepository.deleteAll();
    }

    @Nested
    @DisplayName("Get Internship Tests")
    class GetInternshipTests {

        @Test
        @DisplayName("Should get internship by ID")
        void shouldGetInternshipById() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "SWE Intern");
            Internship created = internshipService.createInternship(request, testUser.getId());

            // Act
            Internship result = internshipService.getInternship(created.getId());

            // Assert
            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
            assertEquals("Google", result.getCompany());
            assertEquals("SWE Intern", result.getRole());
        }

        @Test
        @DisplayName("Should throw exception when internship not found")
        void shouldThrowExceptionWhenInternshipNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.getInternship(nonExistentId)
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("Hide Internship Tests")
    class HideInternshipTests {

        @Test
        @DisplayName("Should hide internship successfully")
        void shouldHideInternshipSuccessfully() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "SWE Intern");
            Internship created = internshipService.createInternship(request, testUser.getId());
            assertFalse(created.isHidden());

            // Act
            internshipService.hideInternship(created.getId(), testUser.getId());

            // Assert
            Internship updated = internshipRepository.findById(created.getId()).orElseThrow();
            assertTrue(updated.isHidden());
        }

        @Test
        @DisplayName("Should fail to hide when not owner")
        void shouldFailToHideWhenNotOwner() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "SWE Intern");
            Internship created = internshipService.createInternship(request, testUser.getId());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.hideInternship(created.getId(), otherUser.getId())
            );
            assertTrue(exception.getMessage().contains("You can only hide your own"));
        }

        @Test
        @DisplayName("Should fail to hide when internship not found")
        void shouldFailToHideWhenInternshipNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.hideInternship(nonExistentId, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("Unhide Internship Tests")
    class UnhideInternshipTests {

        @Test
        @DisplayName("Should unhide internship successfully")
        void shouldUnhideInternshipSuccessfully() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "SWE Intern");
            Internship created = internshipService.createInternship(request, testUser.getId());
            internshipService.hideInternship(created.getId(), testUser.getId());

            // Verify it's hidden
            Internship hidden = internshipRepository.findById(created.getId()).orElseThrow();
            assertTrue(hidden.isHidden());

            // Act
            internshipService.unhideInternship(created.getId(), testUser.getId());

            // Assert
            Internship unhidden = internshipRepository.findById(created.getId()).orElseThrow();
            assertFalse(unhidden.isHidden());
        }

        @Test
        @DisplayName("Should fail to unhide when not owner")
        void shouldFailToUnhideWhenNotOwner() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "SWE Intern");
            Internship created = internshipService.createInternship(request, testUser.getId());
            internshipService.hideInternship(created.getId(), testUser.getId());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.unhideInternship(created.getId(), otherUser.getId())
            );
            assertTrue(exception.getMessage().contains("You can only unhide your own"));
        }

        @Test
        @DisplayName("Should fail to unhide when internship not found")
        void shouldFailToUnhideWhenInternshipNotFound() {
            UUID nonExistentId = UUID.randomUUID();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.unhideInternship(nonExistentId, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("not found"));
        }
    }
}
