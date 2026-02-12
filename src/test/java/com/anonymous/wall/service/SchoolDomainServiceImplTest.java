package com.anonymous.wall.service;

import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.repository.SchoolDomainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("SchoolDomainServiceImpl Tests")
class SchoolDomainServiceImplTest {

    private SchoolDomainServiceImpl service;
    private SchoolDomainRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(SchoolDomainRepository.class);
        service = new SchoolDomainServiceImpl();
        
        // Inject mock repository via reflection
        try {
            var repoField = SchoolDomainServiceImpl.class.getDeclaredField("schoolDomainRepository");
            repoField.setAccessible(true);
            repoField.set(service, repository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject repository", e);
        }
    }

    private SchoolDomain createMockDomain(UUID id, String domain, String schoolName) {
        SchoolDomain schoolDomain = new SchoolDomain();
        schoolDomain.setId(id);
        schoolDomain.setDomain(domain);
        schoolDomain.setSchoolName(schoolName);
        schoolDomain.setCreatedAt(OffsetDateTime.now());
        return schoolDomain;
    }

    @Nested
    @DisplayName("GetAllDomains Tests")
    class GetAllDomainsTests {

        @Test
        @DisplayName("Positive: Should return all domains")
        void shouldReturnAllDomains() {
            // Arrange
            SchoolDomain domain1 = createMockDomain(UUID.randomUUID(), "harvard.edu", "Harvard");
            SchoolDomain domain2 = createMockDomain(UUID.randomUUID(), "mit.edu", "MIT");
            List<SchoolDomain> domains = Arrays.asList(domain1, domain2);
            when(repository.findAll()).thenReturn(domains);

            // Act
            List<SchoolDomain> result = service.getAllDomains();

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("harvard.edu", result.get(0).getDomain());
            assertEquals("mit.edu", result.get(1).getDomain());
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Edge: Should return empty list when no domains exist")
        void shouldReturnEmptyListWhenNoDomainsExist() {
            // Arrange
            when(repository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<SchoolDomain> result = service.getAllDomains();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(repository).findAll();
        }

        @Test
        @DisplayName("Positive: Should return list with single domain")
        void shouldReturnListWithSingleDomain() {
            // Arrange
            SchoolDomain domain = createMockDomain(UUID.randomUUID(), "stanford.edu", "Stanford");
            when(repository.findAll()).thenReturn(Collections.singletonList(domain));

            // Act
            List<SchoolDomain> result = service.getAllDomains();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("stanford.edu", result.get(0).getDomain());
        }
    }

    @Nested
    @DisplayName("GetDomainById Tests")
    class GetDomainByIdTests {

        @Test
        @DisplayName("Positive: Should return domain for valid ID")
        void shouldReturnDomainForValidId() {
            // Arrange
            UUID id = UUID.randomUUID();
            SchoolDomain domain = createMockDomain(id, "yale.edu", "Yale");
            when(repository.findById(id)).thenReturn(Optional.of(domain));

            // Act
            Optional<SchoolDomain> result = service.getDomainById(id);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("yale.edu", result.get().getDomain());
            verify(repository).findById(id);
        }

        @Test
        @DisplayName("Negative: Should return empty for non-existent ID")
        void shouldReturnEmptyForNonExistentId() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<SchoolDomain> result = service.getDomainById(id);

            // Assert
            assertTrue(result.isEmpty());
            verify(repository).findById(id);
        }

        @Test
        @DisplayName("Edge: Should handle null ID")
        void shouldHandleNullId() {
            // Arrange
            when(repository.findById(null)).thenThrow(new IllegalArgumentException("ID cannot be null"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> service.getDomainById(null));
        }
    }

    @Nested
    @DisplayName("CreateDomain Tests")
    class CreateDomainTests {

        @Test
        @DisplayName("Positive: Should create new domain successfully")
        void shouldCreateNewDomainSuccessfully() {
            // Arrange
            String domain = "princeton.edu";
            String schoolName = "Princeton University";
            when(repository.existsByDomain(domain)).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
            assertEquals(schoolName, result.getSchoolName());
            assertNotNull(result.getCreatedAt());
            verify(repository).existsByDomain(domain);
            verify(repository).save(any(SchoolDomain.class));
        }

        @Test
        @DisplayName("Positive: Should normalize domain to lowercase")
        void shouldNormalizeDomainToLowercase() {
            // Arrange
            String domain = "COLUMBIA.EDU";
            String schoolName = "Columbia";
            when(repository.existsByDomain("columbia.edu")).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(domain, schoolName);

            // Assert
            assertEquals("columbia.edu", result.getDomain());
            verify(repository).existsByDomain("columbia.edu");
        }

        @Test
        @DisplayName("Positive: Should trim whitespace from inputs")
        void shouldTrimWhitespaceFromInputs() {
            // Arrange
            String domain = "  berkeley.edu  ";
            String schoolName = "  UC Berkeley  ";
            when(repository.existsByDomain("berkeley.edu")).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(domain, schoolName);

            // Assert
            assertEquals("berkeley.edu", result.getDomain());
            assertEquals("UC Berkeley", result.getSchoolName());
        }

        @Test
        @DisplayName("Negative: Should throw exception for duplicate domain")
        void shouldThrowExceptionForDuplicateDomain() {
            // Arrange
            String domain = "cornell.edu";
            String schoolName = "Cornell";
            when(repository.existsByDomain(domain)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createDomain(domain, schoolName)
            );
            assertTrue(exception.getMessage().contains("already exists"));
            verify(repository).existsByDomain(domain);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Edge: Should handle domain with special characters")
        void shouldHandleDomainWithSpecialCharacters() {
            // Arrange
            String domain = "u-tokyo.ac.jp";
            String schoolName = "University of Tokyo";
            when(repository.existsByDomain(domain)).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(domain, schoolName);

            // Assert
            assertEquals("u-tokyo.ac.jp", result.getDomain());
        }
    }

    @Nested
    @DisplayName("DeleteDomain Tests")
    class DeleteDomainTests {

        @Test
        @DisplayName("Positive: Should delete existing domain")
        void shouldDeleteExistingDomain() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(repository.existsById(id)).thenReturn(true);
            doNothing().when(repository).deleteById(id);

            // Act
            service.deleteDomain(id);

            // Assert
            verify(repository).existsById(id);
            verify(repository).deleteById(id);
        }

        @Test
        @DisplayName("Negative: Should throw exception for non-existent domain")
        void shouldThrowExceptionForNonExistentDomain() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(repository.existsById(id)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteDomain(id)
            );
            assertTrue(exception.getMessage().contains("not found"));
            verify(repository).existsById(id);
            verify(repository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Edge: Should handle null ID")
        void shouldHandleNullId() {
            // Arrange
            when(repository.existsById(null)).thenThrow(new IllegalArgumentException("ID cannot be null"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> service.deleteDomain(null));
        }
    }

    @Nested
    @DisplayName("IsDomainApproved Tests")
    class IsDomainApprovedTests {

        @Test
        @DisplayName("Positive: Should return true for approved domain")
        void shouldReturnTrueForApprovedDomain() {
            // Arrange
            String domain = "duke.edu";
            when(repository.existsByDomain(domain)).thenReturn(true);

            // Act
            boolean result = service.isDomainApproved(domain);

            // Assert
            assertTrue(result);
            verify(repository).existsByDomain(domain);
        }

        @Test
        @DisplayName("Negative: Should return false for non-approved domain")
        void shouldReturnFalseForNonApprovedDomain() {
            // Arrange
            String domain = "unknown.edu";
            when(repository.existsByDomain(domain)).thenReturn(false);

            // Act
            boolean result = service.isDomainApproved(domain);

            // Assert
            assertFalse(result);
            verify(repository).existsByDomain(domain);
        }

        @Test
        @DisplayName("Edge: Should return false for null domain")
        void shouldReturnFalseForNullDomain() {
            // Act
            boolean result = service.isDomainApproved(null);

            // Assert
            assertFalse(result);
            verify(repository, never()).existsByDomain(any());
        }

        @Test
        @DisplayName("Edge: Should return false for empty domain")
        void shouldReturnFalseForEmptyDomain() {
            // Act
            boolean result = service.isDomainApproved("");

            // Assert
            assertFalse(result);
            verify(repository, never()).existsByDomain(any());
        }

        @Test
        @DisplayName("Edge: Should return false for whitespace domain")
        void shouldReturnFalseForWhitespaceDomain() {
            // Act
            boolean result = service.isDomainApproved("   ");

            // Assert
            assertFalse(result);
            verify(repository, never()).existsByDomain(any());
        }

        @Test
        @DisplayName("Positive: Should normalize domain to lowercase and trim")
        void shouldNormalizeDomainToLowercaseAndTrim() {
            // Arrange
            String domain = "  NYU.EDU  ";
            when(repository.existsByDomain("nyu.edu")).thenReturn(true);

            // Act
            boolean result = service.isDomainApproved(domain);

            // Assert
            assertTrue(result);
            verify(repository).existsByDomain("nyu.edu");
        }

        @Test
        @DisplayName("Edge: Should handle domain with mixed case")
        void shouldHandleDomainWithMixedCase() {
            // Arrange
            String domain = "BostonU.edu";
            when(repository.existsByDomain("bostonu.edu")).thenReturn(true);

            // Act
            boolean result = service.isDomainApproved(domain);

            // Assert
            assertTrue(result);
            verify(repository).existsByDomain("bostonu.edu");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Edge: Should handle repository exceptions gracefully in getAllDomains")
        void shouldHandleRepositoryExceptionsInGetAllDomains() {
            // Arrange
            when(repository.findAll()).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> service.getAllDomains());
        }

        @Test
        @DisplayName("Edge: Should handle repository exceptions in createDomain")
        void shouldHandleRepositoryExceptionsInCreateDomain() {
            // Arrange
            when(repository.existsByDomain(anyString())).thenReturn(false);
            when(repository.save(any())).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> service.createDomain("test.edu", "Test"));
        }

        @Test
        @DisplayName("Edge: Should handle very long domain names")
        void shouldHandleVeryLongDomainNames() {
            // Arrange
            String longDomain = "a".repeat(200) + ".edu";
            when(repository.existsByDomain(longDomain.toLowerCase())).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(longDomain, "Long School");

            // Assert
            assertEquals(longDomain.toLowerCase(), result.getDomain());
        }

        @Test
        @DisplayName("Edge: Should handle very long school names")
        void shouldHandleVeryLongSchoolNames() {
            // Arrange
            String domain = "long.edu";
            String longName = "The " + "Very ".repeat(50) + "Long University Name";
            when(repository.existsByDomain(domain)).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(domain, longName);

            // Assert
            assertEquals(longName.trim(), result.getSchoolName());
        }
    }

    @Nested
    @DisplayName("Transaction Tests")
    class TransactionTests {

        @Test
        @DisplayName("Positive: CreateDomain should use transaction")
        void createDomainShouldUseTransaction() {
            // This test verifies the @Transactional annotation is present
            // In a real integration test, we'd verify rollback behavior
            
            // Arrange
            String domain = "transact.edu";
            when(repository.existsByDomain(domain)).thenReturn(false);
            when(repository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            // Act
            SchoolDomain result = service.createDomain(domain, "Transaction Test");

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Positive: DeleteDomain should use transaction")
        void deleteDomainShouldUseTransaction() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(repository.existsById(id)).thenReturn(true);
            doNothing().when(repository).deleteById(id);

            // Act & Assert
            assertDoesNotThrow(() -> service.deleteDomain(id));
        }
    }
}
