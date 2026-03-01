package com.anonymous.wall.service;
import com.anonymous.wall.model.CommentParentType;

import com.anonymous.wall.entity.Comment;
import com.anonymous.wall.entity.Post;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.notification.event.CommentCreatedEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CommentsService Complete Tests")
class CommentsServiceTest {

    private CommentsServiceImpl commentsService;
    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private UserRepository userRepository;
    @SuppressWarnings("unchecked")
    private ApplicationEventPublisher<CommentCreatedEvent> eventPublisher;

    private UUID testUserId;
    private UUID testPostId;
    private Post testPost;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        userRepository = mock(UserRepository.class);
        
        commentsService = new CommentsServiceImpl();
        
        try {
            var commentRepoField = CommentsServiceImpl.class.getDeclaredField("commentRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(commentsService, commentRepository);

            var postRepoField = CommentsServiceImpl.class.getDeclaredField("postRepository");
            postRepoField.setAccessible(true);
            postRepoField.set(commentsService, postRepository);

            var userRepoField = CommentsServiceImpl.class.getDeclaredField("userRepository");
            userRepoField.setAccessible(true);
            userRepoField.set(commentsService, userRepository);

            eventPublisher = mock(ApplicationEventPublisher.class);
            var publisherField = CommentsServiceImpl.class.getDeclaredField("eventPublisher");
            publisherField.setAccessible(true);
            publisherField.set(commentsService, eventPublisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Setup test data
        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        
        testUser = new UserEntity();
        testUser.setId(testUserId);
        testUser.setEmail("test@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setProfileName("TestUser");

        testPost = new Post();
        testPost.setId(testPostId);
        testPost.setWall("national");
        testPost.setUserId(testUserId);
        testPost.setHidden(false);
    }

    @Nested
    @DisplayName("Add Comment - Positive Cases")
    class AddCommentPositiveCases {

        @Test
        @DisplayName("Should add comment to national post successfully")
        void shouldAddCommentToNationalPost() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("This is a test comment");

            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            Comment result = commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).save(any(Comment.class));
            verify(postRepository, times(1)).update(any(Post.class));
        }

        @Test
        @DisplayName("Should add comment to campus post from same school")
        void shouldAddCommentToCampusPostSameSchool() {
            // Arrange
            testPost.setWall("campus");
            testPost.setSchoolDomain("harvard.edu");
            
            CreateCommentRequest request = new CreateCommentRequest("Campus comment");

            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            Comment result = commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            assertNotNull(result);
            verify(commentRepository, times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("Should set profile name on comment")
        void shouldSetProfileNameOnComment() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Test");

            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            when(commentRepository.save(captor.capture())).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            Comment savedComment = captor.getValue();
            assertEquals("TestUser", savedComment.getProfileName());
        }
    }

    @Nested
    @DisplayName("Add Comment - Negative Cases")
    class AddCommentNegativeCases {

        @Test
        @DisplayName("Should throw exception for non-existent post")
        void shouldThrowForNonExistentPost() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Test");
            when(postRepository.findById(testPostId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
            assertTrue(exception.getMessage().contains("Post not found"));
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for empty comment text")
        void shouldThrowForEmptyText() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("");
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should throw exception for null comment text")
        void shouldThrowForNullText() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest(null);
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only text")
        void shouldThrowForWhitespaceOnlyText() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("   ");
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
        }

        @Test
        @DisplayName("Should throw exception for text exceeding max length")
        void shouldThrowForTextTooLong() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("a".repeat(5001));
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
            assertTrue(exception.getMessage().contains("exceeds maximum length"));
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowForNonExistentUser() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Test");
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
            assertTrue(exception.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should throw exception for hidden post")
        void shouldThrowForHiddenPost() {
            // Arrange
            testPost.setHidden(true);
            CreateCommentRequest request = new CreateCommentRequest("Test");
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
        }

        @Test
        @DisplayName("Should throw exception for campus post from different school")
        void shouldThrowForDifferentSchool() {
            // Arrange
            testPost.setWall("campus");
            testPost.setSchoolDomain("mit.edu");
            testUser.setSchoolDomain("harvard.edu");
            
            CreateCommentRequest request = new CreateCommentRequest("Test");
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
            assertTrue(exception.getMessage().contains("other schools"));
        }

        @Test
        @DisplayName("Should throw exception when user has no school domain for campus post")
        void shouldThrowWhenUserHasNoSchoolDomain() {
            // Arrange
            testPost.setWall("campus");
            testPost.setSchoolDomain("harvard.edu");
            testUser.setSchoolDomain(null);
            
            CreateCommentRequest request = new CreateCommentRequest("Test");
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId));
        }
    }

    @Nested
    @DisplayName("Add Comment - Edge Cases")
    class AddCommentEdgeCases {

        @Test
        @DisplayName("Should handle comment at max length (5000 chars)")
        void shouldHandleMaxLengthComment() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("a".repeat(5000));
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            Comment result = commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle comment with special characters")
        void shouldHandleSpecialCharacters() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Test !@#$%^&*()_+-=[]{}|;':\",./<>?");
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            Comment result = commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle comment with Unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("测试评论 こんにちは 🎉🎊");
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            Comment result = commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle comment with newlines")
        void shouldHandleNewlines() {
            // Arrange
            CreateCommentRequest request = new CreateCommentRequest("Line 1\nLine 2\nLine 3");
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                comment.setId(UUID.randomUUID());
                return comment;
            });

            // Act
            Comment result = commentsService.addComment(CommentParentType.POST, testPostId, request, testUserId);

            // Assert
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Hide Comment - Positive Cases")
    class HideCommentPositiveCases {

        @Test
        @DisplayName("Should hide own comment successfully")
        void shouldHideOwnComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", testUserId, "Test");
            comment.setId(commentId);
            comment.setHidden(false);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Comment result = commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            // Assert
            assertTrue(result.isHidden());
            verify(commentRepository, times(1)).update(any(Comment.class));
            verify(postRepository, times(1)).update(any(Post.class));
        }

        @Test
        @DisplayName("Should return comment if already hidden")
        void shouldReturnIfAlreadyHidden() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", testUserId, "Test");
            comment.setId(commentId);
            comment.setHidden(true);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // Act
            Comment result = commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            // Assert
            assertTrue(result.isHidden());
            verify(commentRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Hide Comment - Negative Cases")
    class HideCommentNegativeCases {

        @Test
        @DisplayName("Should throw exception when hiding other user's comment")
        void shouldThrowWhenHidingOthersComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", otherUserId, "Test");
            comment.setId(commentId);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertTrue(exception.getMessage().contains("only hide your own"));
        }

        @Test
        @DisplayName("Should throw exception for non-existent comment")
        void shouldThrowForNonExistentComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId));
        }

        @Test
        @DisplayName("Should throw exception when comment belongs to different post")
        void shouldThrowWhenCommentBelongsToDifferentPost() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            UUID otherPostId = UUID.randomUUID();
            Comment comment = new Comment(otherPostId, "POST", testUserId, "Test");
            comment.setId(commentId);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertTrue(exception.getMessage().contains("does not belong"));
        }
    }

    @Nested
    @DisplayName("Unhide Comment - Positive Cases")
    class UnhideCommentPositiveCases {

        @Test
        @DisplayName("Should unhide own comment successfully")
        void shouldUnhideOwnComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", testUserId, "Test");
            comment.setId(commentId);
            comment.setHidden(true);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Comment result = commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            // Assert
            assertFalse(result.isHidden());
            verify(commentRepository, times(1)).update(any(Comment.class));
            verify(postRepository, times(1)).update(any(Post.class));
        }

        @Test
        @DisplayName("Should return comment if not hidden")
        void shouldReturnIfNotHidden() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", testUserId, "Test");
            comment.setId(commentId);
            comment.setHidden(false);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // Act
            Comment result = commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            // Assert
            assertFalse(result.isHidden());
            verify(commentRepository, never()).update(any());
        }
    }

    @Nested
    @DisplayName("Unhide Comment - Negative Cases")
    class UnhideCommentNegativeCases {

        @Test
        @DisplayName("Should throw exception when unhiding other user's comment")
        void shouldThrowWhenUnhidingOthersComment() {
            // Arrange
            UUID commentId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", otherUserId, "Test");
            comment.setId(commentId);
            comment.setHidden(true);
            
            when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertTrue(exception.getMessage().contains("only unhide your own"));
        }
    }
}
