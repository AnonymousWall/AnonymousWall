package com.anonymous.wall.service;

import com.anonymous.wall.entity.SchoolDomain;
import com.anonymous.wall.repository.SchoolDomainRepository;
import com.anonymous.wall.service.impl.SchoolDomainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SchoolDomainServiceImpl Unit Tests")
class SchoolDomainServiceImplTest {

    private SchoolDomainServiceImpl service;
    private SchoolDomainRepository schoolDomainRepository;

    @BeforeEach
    void setUp() {
        schoolDomainRepository = mock(SchoolDomainRepository.class);
        service = new SchoolDomainServiceImpl();
        try {
            var field = SchoolDomainServiceImpl.class.getDeclaredField("schoolDomainRepository");
            field.setAccessible(true);
            field.set(service, schoolDomainRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private SchoolDomain buildDomain(String domain, String schoolName) {
        SchoolDomain sd = new SchoolDomain();
        sd.setId(UUID.randomUUID());
        sd.setDomain(domain);
        sd.setSchoolName(schoolName);
        sd.setCreatedAt(OffsetDateTime.now());
        return sd;
    }

    // ─── getAllDomains ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllDomains()")
    class GetAllDomainsTests {

        @Test
        @DisplayName("Should return all domains from repository")
        void shouldReturnAllDomains() {
            List<SchoolDomain> domains = List.of(
                    buildDomain("harvard.edu", "Harvard"),
                    buildDomain("mit.edu", "MIT")
            );
            when(schoolDomainRepository.findAll()).thenReturn(domains);

            List<SchoolDomain> result = service.getAllDomains();

            assertEquals(2, result.size());
            verify(schoolDomainRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no domains exist")
        void shouldReturnEmptyList() {
            when(schoolDomainRepository.findAll()).thenReturn(List.of());

            List<SchoolDomain> result = service.getAllDomains();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(schoolDomainRepository.findAll()).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.getAllDomains());
            assertSame(dbError, thrown);
        }
    }

    // ─── getDomainById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDomainById()")
    class GetDomainByIdTests {

        @Test
        @DisplayName("Should return present Optional when domain found")
        void shouldReturnPresentOptional() {
            UUID id = UUID.randomUUID();
            SchoolDomain domain = buildDomain("harvard.edu", "Harvard");
            when(schoolDomainRepository.findById(id)).thenReturn(Optional.of(domain));

            Optional<SchoolDomain> result = service.getDomainById(id);

            assertTrue(result.isPresent());
            assertSame(domain, result.get());
        }

        @Test
        @DisplayName("Should return empty Optional when domain not found")
        void shouldReturnEmptyOptional() {
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.findById(id)).thenReturn(Optional.empty());

            Optional<SchoolDomain> result = service.getDomainById(id);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should pass id to repository unchanged")
        void shouldPassIdUnchanged() {
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.findById(id)).thenReturn(Optional.empty());

            service.getDomainById(id);

            verify(schoolDomainRepository).findById(id);
            verifyNoMoreInteractions(schoolDomainRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(schoolDomainRepository.findById(any())).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.getDomainById(UUID.randomUUID()));
            assertSame(dbError, thrown);
        }
    }

    // ─── createDomain ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createDomain()")
    class CreateDomainTests {

        @Test
        @DisplayName("Should create and return domain when it does not already exist")
        void shouldCreateDomainSuccessfully() {
            SchoolDomain saved = buildDomain("harvard.edu", "Harvard University");
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenReturn(saved);

            SchoolDomain result = service.createDomain("harvard.edu", "Harvard University");

            assertSame(saved, result);
            verify(schoolDomainRepository).save(any());
        }

        @Test
        @DisplayName("Should normalize domain to lowercase before saving")
        void shouldNormalizeDomainToLowercase() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SchoolDomain result = service.createDomain("HARVARD.EDU", "Harvard");

            assertEquals("harvard.edu", result.getDomain());
        }

        @Test
        @DisplayName("Should trim whitespace from domain before saving")
        void shouldTrimDomainWhitespace() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SchoolDomain result = service.createDomain("  harvard.edu  ", "Harvard");

            assertEquals("harvard.edu", result.getDomain());
        }

        @Test
        @DisplayName("Should trim whitespace from schoolName before saving")
        void shouldTrimSchoolNameWhitespace() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SchoolDomain result = service.createDomain("harvard.edu", "  Harvard University  ");

            assertEquals("Harvard University", result.getSchoolName());
        }

        @Test
        @DisplayName("Should normalize mixed-case + whitespace domain before checking existence")
        void shouldCheckExistenceWithNormalizedDomain() {
            when(schoolDomainRepository.existsByDomain("mit.edu")).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createDomain("  MIT.EDU  ", "MIT");

            verify(schoolDomainRepository).existsByDomain("mit.edu");
        }

        @Test
        @DisplayName("Should set createdAt on the saved entity")
        void shouldSetCreatedAt() {
            when(schoolDomainRepository.existsByDomain(any())).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
            SchoolDomain result = service.createDomain("harvard.edu", "Harvard");
            OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

            assertNotNull(result.getCreatedAt());
            assertTrue(result.getCreatedAt().isAfter(before));
            assertTrue(result.getCreatedAt().isBefore(after));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when domain already exists")
        void shouldThrowWhenDomainAlreadyExists() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(true);

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> service.createDomain("harvard.edu", "Harvard"));
            assertTrue(thrown.getMessage().contains("harvard.edu"));
            assertTrue(thrown.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("Should throw with normalized domain in error message")
        void shouldThrowWithNormalizedDomainInMessage() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(true);

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> service.createDomain("HARVARD.EDU", "Harvard"));
            assertTrue(thrown.getMessage().contains("harvard.edu"),
                    "Error message should contain normalized domain, not original casing");
        }

        @Test
        @DisplayName("Should not call save when domain already exists")
        void shouldNotSaveWhenDomainAlreadyExists() {
            when(schoolDomainRepository.existsByDomain(any())).thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.createDomain("harvard.edu", "Harvard"));
            verify(schoolDomainRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate repository exception from save")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(schoolDomainRepository.existsByDomain(any())).thenReturn(false);
            when(schoolDomainRepository.save(any())).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.createDomain("harvard.edu", "Harvard"));
            assertSame(dbError, thrown);
        }
    }

    // ─── deleteDomain ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteDomain()")
    class DeleteDomainTests {

        @Test
        @DisplayName("Should delete domain when it exists")
        void shouldDeleteDomainSuccessfully() {
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.existsById(id)).thenReturn(true);

            assertDoesNotThrow(() -> service.deleteDomain(id));

            verify(schoolDomainRepository).deleteById(id);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when domain not found")
        void shouldThrowWhenDomainNotFound() {
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.existsById(id)).thenReturn(false);

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> service.deleteDomain(id));
            assertEquals("School domain not found", thrown.getMessage());
        }

        @Test
        @DisplayName("Should not call deleteById when domain does not exist")
        void shouldNotDeleteWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.existsById(id)).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () -> service.deleteDomain(id));
            verify(schoolDomainRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should pass correct id to deleteById")
        void shouldPassCorrectIdToDelete() {
            UUID id = UUID.randomUUID();
            when(schoolDomainRepository.existsById(id)).thenReturn(true);

            service.deleteDomain(id);

            verify(schoolDomainRepository).deleteById(id);
        }

        @Test
        @DisplayName("Should propagate repository exception from deleteById")
        void shouldPropagateRepositoryException() {
            UUID id = UUID.randomUUID();
            RuntimeException dbError = new RuntimeException("DB error");
            when(schoolDomainRepository.existsById(id)).thenReturn(true);
            doThrow(dbError).when(schoolDomainRepository).deleteById(id);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.deleteDomain(id));
            assertSame(dbError, thrown);
        }
    }

    // ─── isDomainApproved ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("isDomainApproved()")
    class IsDomainApprovedTests {

        @Test
        @DisplayName("Should return true when domain exists in repository")
        void shouldReturnTrueForApprovedDomain() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(true);

            assertTrue(service.isDomainApproved("harvard.edu"));
        }

        @Test
        @DisplayName("Should return false when domain does not exist in repository")
        void shouldReturnFalseForUnknownDomain() {
            when(schoolDomainRepository.existsByDomain(any())).thenReturn(false);

            assertFalse(service.isDomainApproved("unknown.edu"));
        }

        @Test
        @DisplayName("Should normalize domain to lowercase before checking")
        void shouldNormalizeDomainToLowercase() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(true);

            assertTrue(service.isDomainApproved("HARVARD.EDU"));
            verify(schoolDomainRepository).existsByDomain("harvard.edu");
        }

        @Test
        @DisplayName("Should trim whitespace before checking")
        void shouldTrimWhitespaceBeforeChecking() {
            when(schoolDomainRepository.existsByDomain("harvard.edu")).thenReturn(true);

            assertTrue(service.isDomainApproved("  harvard.edu  "));
            verify(schoolDomainRepository).existsByDomain("harvard.edu");
        }

        @Test
        @DisplayName("Should return false for null domain without calling repository")
        void shouldReturnFalseForNullDomain() {
            assertFalse(service.isDomainApproved(null));
            verifyNoInteractions(schoolDomainRepository);
        }

        @Test
        @DisplayName("Should return false for empty string without calling repository")
        void shouldReturnFalseForEmptyString() {
            assertFalse(service.isDomainApproved(""));
            verifyNoInteractions(schoolDomainRepository);
        }

        @Test
        @DisplayName("Should return false for blank string without calling repository")
        void shouldReturnFalseForBlankString() {
            assertFalse(service.isDomainApproved("   "));
            verifyNoInteractions(schoolDomainRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(schoolDomainRepository.existsByDomain(any())).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.isDomainApproved("harvard.edu"));
            assertSame(dbError, thrown);
        }
    }
}
