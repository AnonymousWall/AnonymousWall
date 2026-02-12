package com.anonymous.wall.repository;

import com.anonymous.wall.entity.SchoolDomain;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("SchoolDomainRepository Tests")
class SchoolDomainRepositoryTest {

    @Inject
    SchoolDomainRepository repository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // Helper method
    private SchoolDomain createTestDomain(String domain, String schoolName) {
        SchoolDomain schoolDomain = new SchoolDomain();
        schoolDomain.setDomain(domain);
        schoolDomain.setSchoolName(schoolName);
        schoolDomain.setCreatedAt(OffsetDateTime.now());
        return repository.save(schoolDomain);
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("Positive: Should save new school domain")
        void shouldSaveNewSchoolDomain() {
            // Arrange
            SchoolDomain domain = new SchoolDomain();
            domain.setDomain("harvard.edu");
            domain.setSchoolName("Harvard University");
            domain.setCreatedAt(OffsetDateTime.now());

            // Act
            SchoolDomain saved = repository.save(domain);

            // Assert
            assertNotNull(saved);
            assertNotNull(saved.getId());
            assertEquals("harvard.edu", saved.getDomain());
            assertEquals("Harvard University", saved.getSchoolName());
            assertNotNull(saved.getCreatedAt());
        }

        @Test
        @DisplayName("Positive: Should auto-generate UUID")
        void shouldAutoGenerateUuid() {
            // Arrange
            SchoolDomain domain = new SchoolDomain();
            domain.setDomain("mit.edu");
            domain.setSchoolName("MIT");
            domain.setCreatedAt(OffsetDateTime.now());

            // Act
            SchoolDomain saved = repository.save(domain);

            // Assert
            assertNotNull(saved.getId());
            assertNotEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), saved.getId());
        }

        @Test
        @DisplayName("Edge: Should handle domain with special characters")
        void shouldHandleDomainWithSpecialCharacters() {
            // Arrange
            SchoolDomain domain = new SchoolDomain();
            domain.setDomain("u-tokyo.ac.jp");
            domain.setSchoolName("University of Tokyo");
            domain.setCreatedAt(OffsetDateTime.now());

            // Act
            SchoolDomain saved = repository.save(domain);

            // Assert
            assertNotNull(saved);
            assertEquals("u-tokyo.ac.jp", saved.getDomain());
        }
    }

    @Nested
    @DisplayName("FindById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Positive: Should find domain by valid ID")
        void shouldFindDomainByValidId() {
            // Arrange
            SchoolDomain saved = createTestDomain("stanford.edu", "Stanford University");

            // Act
            Optional<SchoolDomain> found = repository.findById(saved.getId());

            // Assert
            assertTrue(found.isPresent());
            assertEquals(saved.getId(), found.get().getId());
            assertEquals("stanford.edu", found.get().getDomain());
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act
            Optional<SchoolDomain> found = repository.findById(nonExistentId);

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should handle null ID gracefully")
        void shouldHandleNullId() {
            // Act & Assert
            assertThrows(Exception.class, () -> repository.findById(null));
        }
    }

    @Nested
    @DisplayName("FindByDomain Tests")
    class FindByDomainTests {

        @Test
        @DisplayName("Positive: Should find domain by exact match")
        void shouldFindDomainByExactMatch() {
            // Arrange
            createTestDomain("berkeley.edu", "UC Berkeley");

            // Act
            Optional<SchoolDomain> found = repository.findByDomain("berkeley.edu");

            // Assert
            assertTrue(found.isPresent());
            assertEquals("berkeley.edu", found.get().getDomain());
            assertEquals("UC Berkeley", found.get().getSchoolName());
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent domain")
        void shouldReturnEmptyForNonExistentDomain() {
            // Arrange
            createTestDomain("yale.edu", "Yale University");

            // Act
            Optional<SchoolDomain> found = repository.findByDomain("princeton.edu");

            // Assert
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should be case-sensitive")
        void shouldBeCaseSensitive() {
            // Arrange
            createTestDomain("columbia.edu", "Columbia University");

            // Act
            Optional<SchoolDomain> found = repository.findByDomain("COLUMBIA.EDU");

            // Assert
            // Micronaut Data default is case-sensitive unless specified otherwise
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should handle empty string")
        void shouldHandleEmptyString() {
            // Act
            Optional<SchoolDomain> found = repository.findByDomain("");

            // Assert
            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("ExistsByDomain Tests")
    class ExistsByDomainTests {

        @Test
        @DisplayName("Positive: Should return true for existing domain")
        void shouldReturnTrueForExistingDomain() {
            // Arrange
            createTestDomain("oxford.ac.uk", "Oxford University");

            // Act
            boolean exists = repository.existsByDomain("oxford.ac.uk");

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Negative: Should return false for non-existent domain")
        void shouldReturnFalseForNonExistentDomain() {
            // Arrange
            createTestDomain("cambridge.ac.uk", "Cambridge University");

            // Act
            boolean exists = repository.existsByDomain("imperial.ac.uk");

            // Assert
            assertFalse(exists);
        }

        @Test
        @DisplayName("Edge: Should handle null domain")
        void shouldHandleNullDomain() {
            // Act & Assert - Micronaut Data doesn't accept null for non-nullable parameters
            assertThrows(IllegalArgumentException.class, () -> repository.existsByDomain(null));
        }

        @Test
        @DisplayName("Edge: Should handle empty domain")
        void shouldHandleEmptyDomain() {
            // Act
            boolean exists = repository.existsByDomain("");

            // Assert
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Positive: Should update existing domain")
        void shouldUpdateExistingDomain() {
            // Arrange
            SchoolDomain saved = createTestDomain("upenn.edu", "UPenn");

            // Act
            saved.setSchoolName("University of Pennsylvania");
            SchoolDomain updated = repository.update(saved);

            // Assert
            assertNotNull(updated);
            assertEquals("University of Pennsylvania", updated.getSchoolName());
            assertEquals("upenn.edu", updated.getDomain());
        }

        @Test
        @DisplayName("Positive: Should update domain field")
        void shouldUpdateDomainField() {
            // Arrange
            SchoolDomain saved = createTestDomain("oldomain.edu", "Old School");

            // Act
            saved.setDomain("newdomain.edu");
            SchoolDomain updated = repository.update(saved);

            // Assert
            assertEquals("newdomain.edu", updated.getDomain());
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Positive: Should delete existing domain by ID")
        void shouldDeleteExistingDomainById() {
            // Arrange
            SchoolDomain saved = createTestDomain("cornell.edu", "Cornell University");
            UUID id = saved.getId();

            // Act
            repository.deleteById(id);

            // Assert
            Optional<SchoolDomain> found = repository.findById(id);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Positive: Should delete domain by entity")
        void shouldDeleteDomainByEntity() {
            // Arrange
            SchoolDomain saved = createTestDomain("duke.edu", "Duke University");

            // Act
            repository.delete(saved);

            // Assert
            Optional<SchoolDomain> found = repository.findById(saved.getId());
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Edge: Should handle delete of non-existent ID gracefully")
        void shouldHandleDeleteOfNonExistentId() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> repository.deleteById(nonExistentId));
        }
    }

    @Nested
    @DisplayName("FindAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("Positive: Should find all domains")
        void shouldFindAllDomains() {
            // Arrange
            createTestDomain("nyu.edu", "NYU");
            createTestDomain("bu.edu", "Boston University");
            createTestDomain("usc.edu", "USC");

            // Act
            Iterable<SchoolDomain> all = repository.findAll();

            // Assert
            assertNotNull(all);
            long count = 0;
            for (SchoolDomain domain : all) {
                count++;
            }
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Edge: Should return empty when no domains exist")
        void shouldReturnEmptyWhenNoDomainsExist() {
            // Act
            Iterable<SchoolDomain> all = repository.findAll();

            // Assert
            assertNotNull(all);
            assertFalse(all.iterator().hasNext());
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Positive: Should count all domains correctly")
        void shouldCountAllDomainsCorrectly() {
            // Arrange
            createTestDomain("domain1.edu", "School 1");
            createTestDomain("domain2.edu", "School 2");
            createTestDomain("domain3.edu", "School 3");

            // Act
            long count = repository.count();

            // Assert
            assertEquals(3, count);
        }

        @Test
        @DisplayName("Edge: Should return zero when no domains exist")
        void shouldReturnZeroWhenNoDomainsExist() {
            // Act
            long count = repository.count();

            // Assert
            assertEquals(0, count);
        }
    }
}
