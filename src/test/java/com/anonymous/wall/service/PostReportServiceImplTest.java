package com.anonymous.wall.service;

import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.repository.PostReportRepository;
import com.anonymous.wall.service.impl.PostReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PostReportServiceImpl Unit Tests")
class PostReportServiceImplTest {

    private PostReportServiceImpl service;
    private PostReportRepository postReportRepository;

    @BeforeEach
    void setUp() {
        postReportRepository = mock(PostReportRepository.class);
        service = new PostReportServiceImpl();
        try {
            var field = PostReportServiceImpl.class.getDeclaredField("postReportRepository");
            field.setAccessible(true);
            field.set(service, postReportRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private PostReport buildReport(UUID postId, UUID reporterUserId) {
        PostReport report = new PostReport();
        report.setPostId(postId);
        report.setReporterUserId(reporterUserId);
        return report;
    }

    // ─── existsByPostIdAndReporterUserId ───────────────────────────────────────

    @Nested
    @DisplayName("existsByPostIdAndReporterUserId()")
    class ExistsByPostIdAndReporterUserIdTests {

        @Test
        @DisplayName("Should return true when report exists")
        void shouldReturnTrueWhenReportExists() {
            UUID postId = UUID.randomUUID();
            UUID reporterUserId = UUID.randomUUID();
            when(postReportRepository.existsByPostIdAndReporterUserId(postId, reporterUserId))
                    .thenReturn(true);

            assertTrue(service.existsByPostIdAndReporterUserId(postId, reporterUserId));
        }

        @Test
        @DisplayName("Should return false when report does not exist")
        void shouldReturnFalseWhenReportDoesNotExist() {
            UUID postId = UUID.randomUUID();
            UUID reporterUserId = UUID.randomUUID();
            when(postReportRepository.existsByPostIdAndReporterUserId(postId, reporterUserId))
                    .thenReturn(false);

            assertFalse(service.existsByPostIdAndReporterUserId(postId, reporterUserId));
        }

        @Test
        @DisplayName("Should pass postId and reporterUserId to repository unchanged")
        void shouldPassParametersUnchanged() {
            UUID postId = UUID.randomUUID();
            UUID reporterUserId = UUID.randomUUID();
            when(postReportRepository.existsByPostIdAndReporterUserId(any(), any()))
                    .thenReturn(false);

            service.existsByPostIdAndReporterUserId(postId, reporterUserId);

            verify(postReportRepository).existsByPostIdAndReporterUserId(postId, reporterUserId);
            verifyNoMoreInteractions(postReportRepository);
        }

        @Test
        @DisplayName("Should not confuse postId and reporterUserId — parameter order is preserved")
        void shouldNotConfusePostIdAndReporterUserId() {
            UUID postId = UUID.randomUUID();
            UUID reporterUserId = UUID.randomUUID();
            when(postReportRepository.existsByPostIdAndReporterUserId(any(), any()))
                    .thenReturn(false);

            service.existsByPostIdAndReporterUserId(postId, reporterUserId);

            verify(postReportRepository).existsByPostIdAndReporterUserId(postId, reporterUserId);
            verify(postReportRepository, never())
                    .existsByPostIdAndReporterUserId(reporterUserId, postId);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(postReportRepository.existsByPostIdAndReporterUserId(any(), any()))
                    .thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.existsByPostIdAndReporterUserId(
                            UUID.randomUUID(), UUID.randomUUID()));
            assertSame(dbError, thrown);
        }
    }

    // ─── save ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("Should delegate to repository and return saved entity")
        void shouldDelegateAndReturnSavedEntity() {
            UUID postId = UUID.randomUUID();
            UUID reporterUserId = UUID.randomUUID();
            PostReport input = buildReport(postId, reporterUserId);
            PostReport saved = buildReport(postId, reporterUserId);
            when(postReportRepository.save(input)).thenReturn(saved);

            PostReport result = service.save(input);

            assertSame(saved, result);
            verify(postReportRepository).save(input);
        }

        @Test
        @DisplayName("Should pass entity to repository unchanged — no mutation in service layer")
        void shouldPassEntityUnchanged() {
            PostReport input = buildReport(UUID.randomUUID(), UUID.randomUUID());
            when(postReportRepository.save(any())).thenReturn(input);

            service.save(input);

            verify(postReportRepository).save(input);
            verifyNoMoreInteractions(postReportRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            PostReport input = buildReport(UUID.randomUUID(), UUID.randomUUID());
            RuntimeException dbError = new RuntimeException("DB error");
            when(postReportRepository.save(input)).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.save(input));
            assertSame(dbError, thrown);
        }
    }
}
