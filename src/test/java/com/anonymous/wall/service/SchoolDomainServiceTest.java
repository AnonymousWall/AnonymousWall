package com.anonymous.wall.service;

import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.repository.SchoolDomainRepository;
import com.anonymous.wall.service.impl.SchoolDomainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SchoolDomainService Tests")
class SchoolDomainServiceTest {

    private SchoolDomainServiceImpl schoolDomainService;
    private SchoolDomainRepository schoolDomainRepository;

    @BeforeEach
    void setUp() {
        schoolDomainRepository = mock(SchoolDomainRepository.class);
        schoolDomainService = new SchoolDomainServiceImpl();
        
        try {
            var repoField = SchoolDomainServiceImpl.class.getDeclaredField("schoolDomainRepository");
            repoField.setAccessible(true);
            repoField.set(schoolDomainService, schoolDomainRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Positive Cases - Valid Operations")
    class PositiveCases {

        @Test
        @DisplayName("Should get all domains")
        void shouldGetAllDomains() {
            // Arrange
            SchoolDomain domain1 = createTestDomain("harvard.edu", "Harvard University");
            SchoolDomain domain2 = createTestDomain("mit.edu", "MIT");
            List<SchoolDomain> domains = Arrays.asList(domain1, domain2);
            
            when(schoolDomainRepository.findAll()).thenReturn(domains);

            // Act
            List<SchoolDomain> result = schoolDomainService.getAllDomains();

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains(domain1));
            assertTrue(result.contains(domain2));
            verify(schoolDomainRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should get domain by ID when exists")
        void shouldGetDomainById() {
            // Arrange
            UUID id = UUID.randomUUID();
            SchoolDomain domain = createTestDomain("stanford.edu", "Stanford University");
            when(schoolDomainRepository.findById(id)).thenReturn(Optional.of(domain));

            // Act
            Optional<SchoolDomain> result = schoolDomainService.getDomainById(id);

            // Assert
            assertTrue(result.isPresent());
            assertEquals("stanford.edu", result.get().getDomain());
            verify(schoolDomainRepository, times(1)).findById(id);
        }

        @Test
        @DisplayName("Should create new domain successfully")
        void shouldCreateDomain() {
            // Arrange
            String domain = "yale.edu";
            String schoolName = "Yale University";
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
            assertEquals(schoolName, result.getSchoolName());
            assertNotNull(result.getCreatedAt());
            verify(schoolDomainRepository, times(1)).existsByDomain(domain);
            verify(schoolDomainRepository, times(1)).save(any(SchoolDomain.class));
        }

        @Test
        @DisplayName("Should normalize domain on creation")
        void shouldNormalizeDomainOnCreation() {
            // Arrange
            String domain = "  PRINCETON.EDU  ";
            String schoolName = "  Princeton University  ";
            
            when(schoolDomainRepository.existsByDomain("princeton.edu")).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertEquals("princeton.edu", result.getDomain());
            assertEquals("Princeton University", result.getSchoolName());
            verify(schoolDomainRepository, times(1)).existsByDomain("princeton.edu");
        }

        @Test
        @DisplayName("Should delete domain when exists")
        void shouldDeleteDomain() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.existsById(id)).thenReturn(true);

            // Act
            assertDoesNotThrow(() -> schoolDomainService.deleteDomain(id));

            // Assert
            verify(schoolDomainRepository, times(1)).existsById(id);
            verify(schoolDomainRepository, times(1)).deleteById(id);
        }

        @Test
        @DisplayName("Should confirm approved domain exists")
        void shouldConfirmApprovedDomain() {
            // Arrange
            String domain = "columbia.edu";
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(true);

            // Act
            boolean result = schoolDomainService.isDomainApproved(domain);

            // Assert
            assertTrue(result);
            verify(schoolDomainRepository, times(1)).existsByDomain(domain);
        }

        @Test
        @DisplayName("Should normalize domain when checking approval")
        void shouldNormalizeDomainWhenChecking() {
            // Arrange
            String domain = "  CORNELL.EDU  ";
            when(schoolDomainRepository.existsByDomain("cornell.edu")).thenReturn(true);

            // Act
            boolean result = schoolDomainService.isDomainApproved(domain);

            // Assert
            assertTrue(result);
            verify(schoolDomainRepository, times(1)).existsByDomain("cornell.edu");
        }
    }

    @Nested
    @DisplayName("Negative Cases - Invalid Operations")
    class NegativeCases {

        @Test
        @DisplayName("Should return empty when domain ID not found")
        void shouldReturnEmptyWhenDomainNotFound() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<SchoolDomain> result = schoolDomainService.getDomainById(id);

            // Assert
            assertFalse(result.isPresent());
            verify(schoolDomainRepository, times(1)).findById(id);
        }

        @Test
        @DisplayName("Should throw exception when creating duplicate domain")
        void shouldThrowExceptionForDuplicateDomain() {
            // Arrange
            String domain = "duke.edu";
            String schoolName = "Duke University";
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> schoolDomainService.createDomain(domain, schoolName));
            
            assertTrue(exception.getMessage().contains("already exists"));
            verify(schoolDomainRepository, times(1)).existsByDomain(domain);
            verify(schoolDomainRepository, never()).save(any(SchoolDomain.class));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent domain")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            // Arrange
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.existsById(id)).thenReturn(false);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> schoolDomainService.deleteDomain(id));
            
            assertTrue(exception.getMessage().contains("not found"));
            verify(schoolDomainRepository, times(1)).existsById(id);
            verify(schoolDomainRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should return false for unapproved domain")
        void shouldReturnFalseForUnapprovedDomain() {
            // Arrange
            String domain = "unknown.edu";
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);

            // Act
            boolean result = schoolDomainService.isDomainApproved(domain);

            // Assert
            assertFalse(result);
            verify(schoolDomainRepository, times(1)).existsByDomain(domain);
        }

        @Test
        @DisplayName("Should return false for null domain")
        void shouldReturnFalseForNullDomain() {
            // Act
            boolean result = schoolDomainService.isDomainApproved(null);

            // Assert
            assertFalse(result);
            verify(schoolDomainRepository, never()).existsByDomain(any());
        }

        @Test
        @DisplayName("Should return false for empty domain")
        void shouldReturnFalseForEmptyDomain() {
            // Act
            boolean result = schoolDomainService.isDomainApproved("");

            // Assert
            assertFalse(result);
            verify(schoolDomainRepository, never()).existsByDomain(any());
        }

        @Test
        @DisplayName("Should return false for whitespace-only domain")
        void shouldReturnFalseForWhitespaceDomain() {
            // Act
            boolean result = schoolDomainService.isDomainApproved("   ");

            // Assert
            assertFalse(result);
            verify(schoolDomainRepository, never()).existsByDomain(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases - Boundary Conditions")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty list when no domains exist")
        void shouldHandleEmptyList() {
            // Arrange
            when(schoolDomainRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<SchoolDomain> result = schoolDomainService.getAllDomains();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(schoolDomainRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should handle very long domain name")
        void shouldHandleVeryLongDomain() {
            // Arrange
            String longDomain = "a".repeat(200) + ".university.edu";
            String schoolName = "University";
            
            when(schoolDomainRepository.existsByDomain(longDomain.toLowerCase())).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(longDomain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(longDomain.toLowerCase(), result.getDomain());
        }

        @Test
        @DisplayName("Should handle very long school name")
        void shouldHandleVeryLongSchoolName() {
            // Arrange
            String domain = "test.edu";
            String longSchoolName = "University of ".repeat(50);
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, longSchoolName);

            // Assert
            assertNotNull(result);
            assertEquals(longSchoolName.trim(), result.getSchoolName());
        }

        @Test
        @DisplayName("Should handle single character domain")
        void shouldHandleSingleCharDomain() {
            // Arrange
            String domain = "a.edu";
            String schoolName = "A University";
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
        }

        @Test
        @DisplayName("Should handle domain with multiple subdomains")
        void shouldHandleMultipleSubdomains() {
            // Arrange
            String domain = "cs.engineering.university.edu";
            String schoolName = "CS Department";
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
        }

        @Test
        @DisplayName("Should handle international domain extensions")
        void shouldHandleInternationalDomains() {
            // Arrange
            String domain = "university.co.uk";
            String schoolName = "UK University";
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
        }

        @Test
        @DisplayName("Should handle domain with numbers")
        void shouldHandleDomainWithNumbers() {
            // Arrange
            String domain = "school123.edu";
            String schoolName = "School 123";
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
        }

        @Test
        @DisplayName("Should handle domain with hyphens")
        void shouldHandleDomainWithHyphens() {
            // Arrange
            String domain = "state-university.edu";
            String schoolName = "State University";
            
            when(schoolDomainRepository.existsByDomain(domain)).thenReturn(false);
            when(schoolDomainRepository.save(any(SchoolDomain.class))).thenAnswer(invocation -> {
                SchoolDomain sd = invocation.getArgument(0);
                sd.setId(UUID.randomUUID());
                return sd;
            });

            // Act
            SchoolDomain result = schoolDomainService.createDomain(domain, schoolName);

            // Assert
            assertNotNull(result);
            assertEquals(domain, result.getDomain());
        }

        @Test
        @DisplayName("Should handle case-insensitive duplicate detection")
        void shouldDetectCaseInsensitiveDuplicate() {
            // Arrange
            String domain = "HARVARD.EDU";
            String schoolName = "Harvard";
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(true);

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> schoolDomainService.createDomain(domain, schoolName));
        }

        @Test
        @DisplayName("Should handle large number of domains")
        void shouldHandleLargeNumberOfDomains() {
            // Arrange
            List<SchoolDomain> largeDomainList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                largeDomainList.add(createTestDomain("school" + i + ".edu", "School " + i));
            }
            when(schoolDomainRepository.findAll()).thenReturn(largeDomainList);

            // Act
            List<SchoolDomain> result = schoolDomainService.getAllDomains();

            // Assert
            assertEquals(1000, result.size());
        }
    }

    private SchoolDomain createTestDomain(String domain, String schoolName) {
        SchoolDomain schoolDomain = new SchoolDomain();
        schoolDomain.setId(UUID.randomUUID());
        schoolDomain.setDomain(domain);
        schoolDomain.setSchoolName(schoolName);
        schoolDomain.setCreatedAt(OffsetDateTime.now());
        return schoolDomain;
    }
}
