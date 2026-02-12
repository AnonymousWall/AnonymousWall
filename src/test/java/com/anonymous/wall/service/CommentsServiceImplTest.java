package com.anonymous.wall.service;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("CommentsServiceImpl Comprehensive Tests")
class CommentsServiceImplTest {

    private CommentsServiceImpl service;
    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private UserRepository userRepository;
    private CommentReportRepository commentReportRepository;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        userRepository = mock(UserRepository.class);
        commentReportRepository = mock(CommentReportRepository.class);
        
        service = new CommentsServiceImpl();
        
        // Inject mocks via reflection
        try {
            var commentRepoField = CommentsServiceImpl.class.getDeclaredField("commentRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(service, commentRepository);
            
            var postRepoField = CommentsServiceImpl.class.getDeclaredField("postRepository");
            postRepoField.setAccessible(true);
            postRepoField.set(service, postRepository);
            
            var userRepoField = CommentsServiceImpl.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(service, userRepository);
            
            var commentReportRepoField = CommentsServiceImpl.class.getDeclaredField("commentReportRepository");
            commentReportRepoField.setAccessible(true);
            commentReportRepoField.set(service, commentReportRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
    }

    private Post createMockPost(UUID id, String wall, String schoolDomain, UUID userId, boolean hidden) {
        Post post = new Post();
        post.setId(id);
        post.setWall(wall);
        post.setSchoolDomain(schoolDomain);
        post.setUserId(userId);
        post.setHidden(hidden);
        post.setCommentCount(0);
        post.setCreatedAt(OffsetDateTime.now());
        return post;
    }

    private UserEntity createMockUser(UUID id, String email, String schoolDomain, String profileName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setSchoolDomain(schoolDomain);
        user.setProfileName(profileName);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }

    private Comment createMockComment(UUID id, UUID postId, UUID userId, String text) {
        Comment comment = new Comment(postId, userId, text);
        comment.setId(id);
        comment.setProfileName("Test User");
        comment.setCreatedAt(OffsetDateTime.now());
        comment.setHidden(false);
        return comment;
    }

    @Nested
    @DisplayName("AddComment Tests")
    class AddCommentTests {

        @Test
        @DisplayName("Positive: Should add comment to national post")
        void shouldAddCommentToNationalPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            UserEntity user = createMockUser(userId, "user@harvard.edu", "harvard.edu", "TestUser");
            CreateCommentRequest request = new CreateCommentRequest("Test comment");
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, "Test comment");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            Comment result = service.addComment(postId, request, userId);

            // Assert
            assertNotNull(result);
            assertEquals("Test comment", result.getText());
            verify(commentRepository).save(any(Comment.class));
            verify(postRepository).update(any(Post.class));
        }

        @Test
        @DisplayName("Positive: Should add comment to campus post with same school")
        void shouldAddCommentToCampusPostWithSameSchool() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "campus", "harvard.edu", UUID.randomUUID(), false);
            UserEntity user = createMockUser(userId, "user@harvard.edu", "harvard.edu", "TestUser");
            CreateCommentRequest request = new CreateCommentRequest("Campus comment");
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, "Campus comment");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            Comment result = service.addComment(postId, request, userId);

            // Assert
            assertNotNull(result);
            assertEquals("Campus comment", result.getText());
        }

        @Test
        @DisplayName("Negative: Should reject comment on non-existent post")
        void shouldRejectCommentOnNonExistentPost() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            CreateCommentRequest request = new CreateCommentRequest("Comment");
            
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addComment(postId, request, userId)
            );
            assertTrue(exception.getMessage().contains("Post not found"));
        }

        @Test
        @DisplayName("Negative: Should reject empty comment text")
        void shouldRejectEmptyCommentText() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            CreateCommentRequest request = new CreateCommentRequest("");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addComment(postId, request, userId)
            );
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Negative: Should reject null comment text")
        void shouldRejectNullCommentText() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            CreateCommentRequest request = new CreateCommentRequest(null);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addComment(postId, request, userId)
            );
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Negative: Should reject comment exceeding max length")
        void shouldRejectCommentExceedingMaxLength() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            CreateCommentRequest request = new CreateCommentRequest("a".repeat(5001)); // Exceeds 5000 character limit
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addComment(postId, request, userId)
            );
            assertTrue(exception.getMessage().contains("exceeds maximum length"));
        }

        @Test
        @DisplayName("Edge: Should accept comment at max length (5000 chars)")
        void shouldAcceptCommentAtMaxLength() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            UserEntity user = createMockUser(userId, "user@test.edu", "test.edu", "TestUser");
            String maxLengthText = "a".repeat(5000);
            CreateCommentRequest request = new CreateCommentRequest(maxLengthText);
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, maxLengthText);
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            Comment result = service.addComment(postId, request, userId);

            // Assert
            assertNotNull(result);
            assertEquals(5000, result.getText().length());
        }

        @Test
        @DisplayName("Edge: Should handle whitespace-only comment as empty")
        void shouldHandleWhitespaceOnlyCommentAsEmpty() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            CreateCommentRequest request = new CreateCommentRequest("   ");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addComment(postId, request, userId)
            );
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Negative: Should reject comment from non-existent user")
        void shouldRejectCommentFromNonExistentUser() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            CreateCommentRequest request = new CreateCommentRequest("Valid comment");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.addComment(postId, request, userId)
            );
            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Positive: Should increment post comment count")
        void shouldIncrementPostCommentCount() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            post.setCommentCount(5);
            UserEntity user = createMockUser(userId, "user@test.edu", "test.edu", "TestUser");
            CreateCommentRequest request = new CreateCommentRequest("Test comment");
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, "Test comment");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            service.addComment(postId, request, userId);

            // Assert
            verify(postRepository).update(argThat(p -> p.getCommentCount() == 6));
        }

        @Test
        @DisplayName("Positive: Should set profile name on comment")
        void shouldSetProfileNameOnComment() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            UserEntity user = createMockUser(userId, "user@test.edu", "test.edu", "MyProfileName");
            CreateCommentRequest request = new CreateCommentRequest("Test");
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, "Test");
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            service.addComment(postId, request, userId);

            // Assert
            verify(commentRepository).save(argThat(c -> 
                c.getProfileName() != null && c.getProfileName().equals("MyProfileName")
            ));
        }
    }

    @Nested
    @DisplayName("GetCommentsWithPagination Tests")
    class GetCommentsWithPaginationTests {

        @Test
        @DisplayName("Positive: Should get comments with default sorting (NEWEST)")
        void shouldGetCommentsWithDefaultSorting() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            
            when(commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable))
                .thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getCommentsWithPagination(postId, pageable, null);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable);
        }

        @Test
        @DisplayName("Positive: Should get comments sorted by NEWEST")
        void shouldGetCommentsSortedByNewest() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            
            when(commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable))
                .thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getCommentsWithPagination(postId, pageable, SortBy.NEWEST);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable);
        }

        @Test
        @DisplayName("Positive: Should get comments sorted by OLDEST")
        void shouldGetCommentsSortedByOldest() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> mockPage = mock(Page.class);
            
            when(commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtAsc(postId, pageable))
                .thenReturn(mockPage);

            // Act
            Page<Comment> result = service.getCommentsWithPagination(postId, pageable, SortBy.OLDEST);

            // Assert
            assertNotNull(result);
            verify(commentRepository).findByPostIdAndHiddenFalseOrderByCreatedAtAsc(postId, pageable);
        }

        @Test
        @DisplayName("Edge: Should handle empty result set")
        void shouldHandleEmptyResultSet() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 10);
            Page<Comment> emptyPage = Page.empty();
            
            when(commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, pageable))
                .thenReturn(emptyPage);

            // Act
            Page<Comment> result = service.getCommentsWithPagination(postId, pageable, SortBy.NEWEST);

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getContent().size());
        }

        @Test
        @DisplayName("Positive: Should support pagination")
        void shouldSupportPagination() {
            // Arrange
            UUID postId = UUID.randomUUID();
            Pageable page1 = Pageable.from(0, 10);
            Pageable page2 = Pageable.from(1, 10);
            Page<Comment> mockPage1 = mock(Page.class);
            Page<Comment> mockPage2 = mock(Page.class);
            
            when(commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, page1))
                .thenReturn(mockPage1);
            when(commentRepository.findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, page2))
                .thenReturn(mockPage2);

            // Act
            Page<Comment> result1 = service.getCommentsWithPagination(postId, page1, SortBy.NEWEST);
            Page<Comment> result2 = service.getCommentsWithPagination(postId, page2, SortBy.NEWEST);

            // Assert
            assertNotNull(result1);
            assertNotNull(result2);
            verify(commentRepository).findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, page1);
            verify(commentRepository).findByPostIdAndHiddenFalseOrderByCreatedAtDesc(postId, page2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Edge: Should handle very long comment text (exactly 5000 chars)")
        void shouldHandleVeryLongCommentText() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            UserEntity user = createMockUser(userId, "user@test.edu", "test.edu", "TestUser");
            CreateCommentRequest request = new CreateCommentRequest("x".repeat(5000));
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, request.getText());
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            Comment result = service.addComment(postId, request, userId);

            // Assert
            assertNotNull(result);
            assertEquals(5000, result.getText().length());
        }

        @Test
        @DisplayName("Edge: Should handle special characters in comment")
        void shouldHandleSpecialCharactersInComment() {
            // Arrange
            UUID postId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Post post = createMockPost(postId, "national", null, UUID.randomUUID(), false);
            UserEntity user = createMockUser(userId, "user@test.edu", "test.edu", "TestUser");
            CreateCommentRequest request = new CreateCommentRequest("Test @#$%^&*() 你好 emoji 😀🎉");
            
            Comment savedComment = createMockComment(UUID.randomUUID(), postId, userId, request.getText());
            
            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
            when(postRepository.update(any(Post.class))).thenReturn(post);

            // Act
            Comment result = service.addComment(postId, request, userId);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Edge: Should handle UUID edge cases")
        void shouldHandleUuidEdgeCases() {
            // Arrange - UUID with all zeros
            UUID postId = UUID.fromString("00000000-0000-0000-0000-000000000000");
            UUID userId = UUID.randomUUID();
            CreateCommentRequest request = new CreateCommentRequest("Test");
            
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class, 
                () -> service.addComment(postId, request, userId));
        }
    }
}
