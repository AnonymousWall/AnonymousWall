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

import java.time.LocalDate;

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
    @DisplayName("Should list all internships with pagination")
    void shouldListInternshipsWithPagination() {
        // Create multiple internships
        CreateInternshipRequest request1 = new CreateInternshipRequest("Google", "SWE Intern");
        CreateInternshipRequest request2 = new CreateInternshipRequest("Microsoft", "PM Intern");
        CreateInternshipRequest request3 = new CreateInternshipRequest("Amazon", "Data Intern");

        internshipService.createInternship(request1, testUser.getId());
        Thread.sleep(10); // Small delay to ensure different timestamps
        internshipService.createInternship(request2, testUser.getId());
        Thread.sleep(10);
        internshipService.createInternship(request3, testUser.getId());

        // Act
        Pageable pageable = Pageable.from(0, 10);
        Page<Internship> result = internshipService.listInternships(pageable);

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
    @DisplayName("Should return empty page when no internships exist")
    void shouldReturnEmptyPageWhenNoInternships() {
        // Act
        Pageable pageable = Pageable.from(0, 10);
        Page<Internship> result = internshipService.listInternships(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalSize());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Should paginate results correctly")
    void shouldPaginateResultsCorrectly() throws InterruptedException {
        // Create 5 internships
        for (int i = 1; i <= 5; i++) {
            CreateInternshipRequest request = new CreateInternshipRequest("Company" + i, "Role" + i);
            internshipService.createInternship(request, testUser.getId());
            Thread.sleep(10);
        }

        // Act - Get first page with 2 items
        Pageable pageable1 = Pageable.from(0, 2);
        Page<Internship> page1 = internshipService.listInternships(pageable1);

        // Act - Get second page with 2 items
        Pageable pageable2 = Pageable.from(1, 2);
        Page<Internship> page2 = internshipService.listInternships(pageable2);

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
