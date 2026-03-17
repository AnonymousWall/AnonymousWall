package com.anonymous.wall.service;

import com.anonymous.wall.entity.PostLike;
import com.anonymous.wall.repository.PostLikeRepository;
import com.anonymous.wall.service.impl.PostLikeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PostLikeServiceImpl Unit Tests")
class PostLikeServiceImplTest {

    private PostLikeServiceImpl service;
    private PostLikeRepository postLikeRepository;

    @BeforeEach
    void setUp() {
        postLikeRepository = mock(PostLikeRepository.class);
        service = new PostLikeServiceImpl();
        try {
            var field = PostLikeServiceImpl.class.getDeclaredField("postLikeRepository");
            field.setAccessible(true);
            field.set(service, postLikeRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private PostLike buildLike(UUID postId, UUID userId) {
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        return like;
    }

    // ─── findByPostIdAndUserId ─────────────────────────────────────────────────

    @Nested
    @DisplayName("findByPostIdAndUserId()")
    class FindByPostIdAndUserIdTests {

        @Test
        @DisplayName("Should return present Optional when like exists")
        void shouldReturnPresentOptionalWhenFound() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            PostLike like = buildLike(postId, userId);
            when(postLikeRepository.findByPostIdAndUserId(postId, userId))
                    .thenReturn(Optional.of(like));

            Optional<PostLike> result = service.findByPostIdAndUserId(postId, userId);

            assertTrue(result.isPresent());
            assertSame(like, result.get());
        }

        @Test
        @DisplayName("Should return empty Optional when like does not exist")
        void shouldReturnEmptyOptionalWhenNotFound() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(postLikeRepository.findByPostIdAndUserId(postId, userId))
                    .thenReturn(Optional.empty());

            Optional<PostLike> result = service.findByPostIdAndUserId(postId, userId);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should pass postId and userId to repository unchanged")
        void shouldPassParametersUnchanged() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(postLikeRepository.findByPostIdAndUserId(any(), any()))
                    .thenReturn(Optional.empty());

            service.findByPostIdAndUserId(postId, userId);

            verify(postLikeRepository).findByPostIdAndUserId(postId, userId);
            verifyNoMoreInteractions(postLikeRepository);
        }

        @Test
        @DisplayName("Should not confuse postId and userId — parameter order is preserved")
        void shouldNotConfusePostIdAndUserId() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(postLikeRepository.findByPostIdAndUserId(any(), any()))
                    .thenReturn(Optional.empty());

            service.findByPostIdAndUserId(postId, userId);

            // If the impl accidentally swapped the arguments this verify would fail
            verify(postLikeRepository).findByPostIdAndUserId(postId, userId);
            verify(postLikeRepository, never()).findByPostIdAndUserId(userId, postId);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(postLikeRepository.findByPostIdAndUserId(any(), any())).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.findByPostIdAndUserId(UUID.randomUUID(), UUID.randomUUID()));
            assertSame(dbError, thrown);
        }
    }

    // ─── deleteByPostIdAndUserId ───────────────────────────────────────────────

    @Nested
    @DisplayName("deleteByPostIdAndUserId()")
    class DeleteByPostIdAndUserIdTests {

        @Test
        @DisplayName("Should delegate delete to repository")
        void shouldDelegateDeleteToRepository() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            assertDoesNotThrow(() -> service.deleteByPostIdAndUserId(postId, userId));

            verify(postLikeRepository).deleteByPostIdAndUserId(postId, userId);
            verifyNoMoreInteractions(postLikeRepository);
        }

        @Test
        @DisplayName("Should not throw when no like exists for the given pair — idempotent")
        void shouldNotThrowWhenLikeDoesNotExist() {
            assertDoesNotThrow(() ->
                    service.deleteByPostIdAndUserId(UUID.randomUUID(), UUID.randomUUID()));
        }

        @Test
        @DisplayName("Should not confuse postId and userId — parameter order is preserved")
        void shouldNotConfusePostIdAndUserId() {
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.deleteByPostIdAndUserId(postId, userId);

            verify(postLikeRepository).deleteByPostIdAndUserId(postId, userId);
            verify(postLikeRepository, never()).deleteByPostIdAndUserId(userId, postId);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            doThrow(dbError).when(postLikeRepository).deleteByPostIdAndUserId(any(), any());

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.deleteByPostIdAndUserId(UUID.randomUUID(), UUID.randomUUID()));
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
            UUID userId = UUID.randomUUID();
            PostLike input = buildLike(postId, userId);
            PostLike saved = buildLike(postId, userId);
            when(postLikeRepository.save(input)).thenReturn(saved);

            PostLike result = service.save(input);

            assertSame(saved, result);
            verify(postLikeRepository).save(input);
        }

        @Test
        @DisplayName("Should pass entity to repository unchanged — no mutation in service layer")
        void shouldPassEntityUnchanged() {
            PostLike input = buildLike(UUID.randomUUID(), UUID.randomUUID());
            when(postLikeRepository.save(any())).thenReturn(input);

            service.save(input);

            verify(postLikeRepository).save(input);
            verifyNoMoreInteractions(postLikeRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            PostLike input = buildLike(UUID.randomUUID(), UUID.randomUUID());
            RuntimeException dbError = new RuntimeException("DB error");
            when(postLikeRepository.save(input)).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.save(input));
            assertSame(dbError, thrown);
        }
    }

    // ─── findByUserIdAndPostIdIn ───────────────────────────────────────────────

    @Nested
    @DisplayName("findByUserIdAndPostIdIn()")
    class FindByUserIdAndPostIdInTests {

        @Test
        @DisplayName("Should return matching likes from repository")
        void shouldReturnMatchingLikes() {
            UUID userId = UUID.randomUUID();
            UUID postId1 = UUID.randomUUID();
            UUID postId2 = UUID.randomUUID();
            List<UUID> postIds = List.of(postId1, postId2);
            List<PostLike> likes = List.of(buildLike(postId1, userId), buildLike(postId2, userId));
            when(postLikeRepository.findByUserIdAndPostIdIn(userId, postIds)).thenReturn(likes);

            List<PostLike> result = service.findByUserIdAndPostIdIn(userId, postIds);

            assertEquals(2, result.size());
            assertSame(likes, result);
        }

        @Test
        @DisplayName("Should return empty list when user has not liked any of the given posts")
        void shouldReturnEmptyListWhenNoMatches() {
            UUID userId = UUID.randomUUID();
            List<UUID> postIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            when(postLikeRepository.findByUserIdAndPostIdIn(userId, postIds))
                    .thenReturn(Collections.emptyList());

            List<PostLike> result = service.findByUserIdAndPostIdIn(userId, postIds);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when postIds list is empty")
        void shouldReturnEmptyListWhenPostIdsEmpty() {
            UUID userId = UUID.randomUUID();
            when(postLikeRepository.findByUserIdAndPostIdIn(userId, Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            List<PostLike> result = service.findByUserIdAndPostIdIn(userId, Collections.emptyList());

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should pass userId and postIds to repository unchanged")
        void shouldPassParametersUnchanged() {
            UUID userId = UUID.randomUUID();
            List<UUID> postIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            when(postLikeRepository.findByUserIdAndPostIdIn(any(), any()))
                    .thenReturn(Collections.emptyList());

            service.findByUserIdAndPostIdIn(userId, postIds);

            verify(postLikeRepository).findByUserIdAndPostIdIn(userId, postIds);
            verifyNoMoreInteractions(postLikeRepository);
        }

        @Test
        @DisplayName("Should propagate repository exception")
        void shouldPropagateRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            when(postLikeRepository.findByUserIdAndPostIdIn(any(), any())).thenThrow(dbError);

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> service.findByUserIdAndPostIdIn(UUID.randomUUID(), List.of(UUID.randomUUID())));
            assertSame(dbError, thrown);
        }
    }
}
