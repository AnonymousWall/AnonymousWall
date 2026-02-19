package com.anonymous.wall.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.repository.InternshipRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("InternshipServiceImpl - Create Internship Tests")
class InternshipServiceImplCreateInternshipTest {

    @Inject
    private InternshipService internshipService;

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    private UserRepository userRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Clean up
        internshipRepository.deleteAll();

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
        internshipRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create Internship - Positive Cases")
    class CreateInternshipPositiveTests {

        @Test
        @DisplayName("Should create internship with all fields")
        void shouldCreateInternshipWithAllFields() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "Software Engineer Intern");
            request.setSalary("$8000/month");
            request.setLocation("Mountain View, CA");
            request.setDescription("Work on cutting-edge projects");
            request.setDeadline(LocalDate.of(2026, 6, 30));

            // Act
            Internship result = internshipService.createInternship(request, testUser.getId());

            // Assert
            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Google", result.getCompany());
            assertEquals("Software Engineer Intern", result.getRole());
            assertEquals("$8000/month", result.getSalary());
            assertEquals("Mountain View, CA", result.getLocation());
            assertEquals("Work on cutting-edge projects", result.getDescription());
            assertEquals(LocalDate.of(2026, 6, 30), result.getDeadline());
            assertEquals(testUser.getId(), result.getUserId());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create internship with minimum required fields")
        void shouldCreateInternshipWithMinimumFields() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("Microsoft", "Data Science Intern");

            // Act
            Internship result = internshipService.createInternship(request, testUser.getId());

            // Assert
            assertNotNull(result);
            assertEquals("Microsoft", result.getCompany());
            assertEquals("Data Science Intern", result.getRole());
            assertNull(result.getSalary());
            assertNull(result.getLocation());
            assertNull(result.getDescription());
            assertNotNull(result.getDeadline());
        }

        @Test
        @DisplayName("Should trim company and role whitespace")
        void shouldTrimWhitespace() {
            // Arrange
            CreateInternshipRequest request = new CreateInternshipRequest("  Apple  ", "  iOS Intern  ");

            // Act
            Internship result = internshipService.createInternship(request, testUser.getId());

            // Assert
            assertEquals("Apple", result.getCompany());
            assertEquals("iOS Intern", result.getRole());
        }
    }

    @Nested
    @DisplayName("Create Internship - Validation Tests")
    class CreateInternshipValidationTests {

        @Test
        @DisplayName("Should fail when company is null")
        void shouldFailWhenCompanyNull() {
            CreateInternshipRequest request = new CreateInternshipRequest(null, "Engineer");

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("Company is required"));
        }

        @Test
        @DisplayName("Should fail when company is empty")
        void shouldFailWhenCompanyEmpty() {
            CreateInternshipRequest request = new CreateInternshipRequest("   ", "Engineer");

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("Company is required"));
        }

        @Test
        @DisplayName("Should fail when role is null")
        void shouldFailWhenRoleNull() {
            CreateInternshipRequest request = new CreateInternshipRequest("Google", null);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("Role is required"));
        }

        @Test
        @DisplayName("Should fail when role is empty")
        void shouldFailWhenRoleEmpty() {
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "   ");

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("Role is required"));
        }

        @Test
        @DisplayName("Should fail when company exceeds 255 characters")
        void shouldFailWhenCompanyTooLong() {
            String longCompany = "A".repeat(256);
            CreateInternshipRequest request = new CreateInternshipRequest(longCompany, "Engineer");

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("Company name cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should fail when role exceeds 255 characters")
        void shouldFailWhenRoleTooLong() {
            String longRole = "A".repeat(256);
            CreateInternshipRequest request = new CreateInternshipRequest("Google", longRole);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, testUser.getId())
            );
            assertTrue(exception.getMessage().contains("Role cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should fail when user does not exist")
        void shouldFailWhenUserNotFound() {
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "Engineer");
            UUID nonExistentUserId = UUID.randomUUID();

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internshipService.createInternship(request, nonExistentUserId)
            );
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }
}
