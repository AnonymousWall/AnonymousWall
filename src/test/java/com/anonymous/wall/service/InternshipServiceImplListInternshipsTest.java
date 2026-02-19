package com.anonymous.wall.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.repository.InternshipRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("InternshipServiceImpl - List Internships Tests")
class InternshipServiceImplListInternshipsTest {

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

    @Test
    @DisplayName("Should list all internships with pagination sorted by newest")
    void shouldListInternshipsWithPagination() {
        // Create multiple internships with explicit timestamps
        CreateInternshipRequest request1 = new CreateInternshipRequest("Google", "SWE Intern");
        CreateInternshipRequest request2 = new CreateInternshipRequest("Microsoft", "PM Intern");
        CreateInternshipRequest request3 = new CreateInternshipRequest("Amazon", "Data Intern");

        Internship internship1 = internshipService.createInternship(request1, testUser.getId());
        Internship internship2 = internshipService.createInternship(request2, testUser.getId());
        Internship internship3 = internshipService.createInternship(request3, testUser.getId());

        // Manually adjust timestamps to ensure ordering
        internship1.setCreatedAt(internship1.getCreatedAt().minusSeconds(2));
        internship2.setCreatedAt(internship2.getCreatedAt().minusSeconds(1));
        internshipRepository.update(internship1);
        internshipRepository.update(internship2);

        // Act
        Pageable pageable = Pageable.from(0, 10);
        Page<Internship> result = internshipService.listInternships(pageable, "newest");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalSize());
        assertEquals(3, result.getContent().size());
        // Should be sorted by newest first
        assertEquals("Amazon", result.getContent().get(0).getCompany());
        assertEquals("Microsoft", result.getContent().get(1).getCompany());
        assertEquals("Google", result.getContent().get(2).getCompany());
    }

    @Test
    @DisplayName("Should list internships sorted by oldest")
    void shouldListInternshipsSortedByOldest() {
        // Create multiple internships
        CreateInternshipRequest request1 = new CreateInternshipRequest("Google", "SWE Intern");
        CreateInternshipRequest request2 = new CreateInternshipRequest("Microsoft", "PM Intern");

        Internship internship1 = internshipService.createInternship(request1, testUser.getId());
        Internship internship2 = internshipService.createInternship(request2, testUser.getId());

        internship1.setCreatedAt(internship1.getCreatedAt().minusSeconds(1));
        internshipRepository.update(internship1);

        // Act
        Pageable pageable = Pageable.from(0, 10);
        Page<Internship> result = internshipService.listInternships(pageable, "oldest");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalSize());
        // Should be sorted by oldest first
        assertEquals("Google", result.getContent().get(0).getCompany());
        assertEquals("Microsoft", result.getContent().get(1).getCompany());
    }

    @Test
    @DisplayName("Should not show hidden internships in list")
    void shouldNotShowHiddenInternships() {
        // Create internships
        CreateInternshipRequest request1 = new CreateInternshipRequest("Google", "SWE Intern");
        CreateInternshipRequest request2 = new CreateInternshipRequest("Microsoft", "PM Intern");

        Internship internship1 = internshipService.createInternship(request1, testUser.getId());
        internshipService.createInternship(request2, testUser.getId());

        // Hide first internship
        internshipService.hideInternship(internship1.getId(), testUser.getId());

        // Act
        Pageable pageable = Pageable.from(0, 10);
        Page<Internship> result = internshipService.listInternships(pageable, "newest");

        // Assert - Only non-hidden internship should be shown
        assertNotNull(result);
        assertEquals(1, result.getTotalSize());
        assertEquals("Microsoft", result.getContent().get(0).getCompany());
    }

    @Test
    @DisplayName("Should return empty page when no internships exist")
    void shouldReturnEmptyPageWhenNoInternships() {
        // Act
        Pageable pageable = Pageable.from(0, 10);
        Page<Internship> result = internshipService.listInternships(pageable, "newest");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalSize());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Should paginate results correctly")
    void shouldPaginateResultsCorrectly() {
        // Create 5 internships with controlled timestamps
        for (int i = 1; i <= 5; i++) {
            CreateInternshipRequest request = new CreateInternshipRequest("Company" + i, "Role" + i);
            Internship internship = internshipService.createInternship(request, testUser.getId());
            // Set timestamps in reverse order so Company5 is newest
            internship.setCreatedAt(internship.getCreatedAt().minusSeconds(5 - i));
            internshipRepository.update(internship);
        }

        // Act - Get first page with 2 items
        Pageable pageable1 = Pageable.from(0, 2);
        Page<Internship> page1 = internshipService.listInternships(pageable1, "newest");

        // Act - Get second page with 2 items
        Pageable pageable2 = Pageable.from(1, 2);
        Page<Internship> page2 = internshipService.listInternships(pageable2, "newest");

        // Assert
        assertEquals(5, page1.getTotalSize());
        assertEquals(2, page1.getContent().size());
        assertEquals(5, page2.getTotalSize());
        assertEquals(2, page2.getContent().size());
        
        // Verify pagination is working (newest first)
        assertEquals("Company5", page1.getContent().get(0).getCompany());
        assertEquals("Company4", page1.getContent().get(1).getCompany());
        assertEquals("Company3", page2.getContent().get(0).getCompany());
        assertEquals("Company2", page2.getContent().get(1).getCompany());
    }
}
