package com.anonymous.wall.service;

import com.anonymous.wall.entity.*;
import com.anonymous.wall.model.CommentParentType;
import com.anonymous.wall.model.CreateCommentRequest;
import com.anonymous.wall.model.SortBy;
import com.anonymous.wall.notification.event.CommentCreatedEvent;
import com.anonymous.wall.notification.event.InternshipCommentCreatedEvent;
import com.anonymous.wall.notification.event.MarketplaceCommentCreatedEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.service.base.*;
import com.anonymous.wall.service.impl.CommentsServiceImpl;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("CommentsService Unit Tests")
class CommentsServiceTest {

    private CommentsServiceImpl commentsService;
    private CommentRepository commentRepository;
    private PostsService postsService;
    private InternshipService internshipService;
    private MarketplaceService marketplaceService;
    private UserService userService;
    private CommentReportService commentReportService;
    private UserBlockService userBlockService;
    @SuppressWarnings("unchecked")
    private ApplicationEventPublisher<CommentCreatedEvent> eventPublisher;
    @SuppressWarnings("unchecked")
    private ApplicationEventPublisher<InternshipCommentCreatedEvent> internshipCommentEventPublisher;
    @SuppressWarnings("unchecked")
    private ApplicationEventPublisher<MarketplaceCommentCreatedEvent> marketplaceCommentEventPublisher;

    private UUID testUserId;
    private UUID testPostId;
    private UUID testInternshipId;
    private UUID testItemId;
    private Post testPost;
    private Internship testInternship;
    private MarketplaceItem testItem;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postsService = mock(PostsService.class);
        internshipService = mock(InternshipService.class);
        marketplaceService = mock(MarketplaceService.class);
        userService = mock(UserService.class);
        commentReportService = mock(CommentReportService.class);
        userBlockService = mock(UserBlockService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        internshipCommentEventPublisher = mock(ApplicationEventPublisher.class);
        marketplaceCommentEventPublisher = mock(ApplicationEventPublisher.class);

        commentsService = new CommentsServiceImpl();

        try {
            setField("commentRepository", commentRepository);

            @SuppressWarnings("unchecked")
            Provider<PostsService> postsServiceProvider = mock(Provider.class);
            when(postsServiceProvider.get()).thenReturn(postsService);
            setField("postsServiceProvider", postsServiceProvider);

            setField("internshipService", internshipService);
            setField("marketplaceService", marketplaceService);
            setField("userService", userService);
            setField("commentReportService", commentReportService);
            setField("userBlockService", userBlockService);
            setField("eventPublisher", eventPublisher);
            setField("internshipCommentEventPublisher", internshipCommentEventPublisher);
            setField("marketplaceCommentEventPublisher", marketplaceCommentEventPublisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        testInternshipId = UUID.randomUUID();
        testItemId = UUID.randomUUID();

        testUser = new UserEntity();
        testUser.setId(testUserId);
        testUser.setEmail("test@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setProfileName("TestUser");

        testPost = new Post();
        testPost.setId(testPostId);
        testPost.setWall("national");
        testPost.setUserId(UUID.randomUUID());
        testPost.setHidden(false);

        testInternship = new Internship();
        testInternship.setId(testInternshipId);
        testInternship.setWall("national");
        testInternship.setUserId(UUID.randomUUID());
        testInternship.setHidden(false);

        testItem = new MarketplaceItem();
        testItem.setId(testItemId);
        testItem.setWall("national");
        testItem.setUserId(UUID.randomUUID());
        testItem.setHidden(false);
    }

    private void setField(String name, Object value) throws Exception {
        var field = CommentsServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(commentsService, value);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void stubSaveReturnsWithId() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
    }

    private Comment buildComment(UUID parentId, UUID userId) {
        Comment c = new Comment(parentId, "POST", userId, "Test comment");
        c.setId(UUID.randomUUID());
        c.setHidden(false);
        return c;
    }

    // ─── Add Comment — Positive ────────────────────────────────────────────────

    @Nested
    @DisplayName("Add Comment — Positive Cases")
    class AddCommentPositiveCases {

        @Test
        @DisplayName("Should add comment to national post successfully")
        void shouldAddCommentToNationalPost() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            Comment result = commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Test comment"), testUserId);

            assertNotNull(result);
            assertEquals(testPostId, result.getParentId());
            assertEquals(testUserId, result.getUserId());
            verify(commentRepository).save(any(Comment.class));
            verify(postsService).update(any(Post.class));
        }

        @Test
        @DisplayName("Should add comment to campus post when user is from same school")
        void shouldAddCommentToCampusPostSameSchool() {
            testPost.setWall("campus");
            testPost.setSchoolDomain("harvard.edu");

            // Impl calls userService.findById TWICE for campus wall:
            // once in validateParentVisibility, once to get profileName.
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            Comment result = commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Campus comment"), testUserId);

            assertNotNull(result);
            verify(userService, times(2)).findById(testUserId);
        }

        @Test
        @DisplayName("Should add comment to internship successfully")
        void shouldAddCommentToInternship() {
            when(internshipService.findById(testInternshipId)).thenReturn(Optional.of(testInternship));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            Comment result = commentsService.addComment(CommentParentType.INTERNSHIP, testInternshipId,
                    new CreateCommentRequest("Internship comment"), testUserId);

            assertNotNull(result);
            verify(commentRepository).save(any(Comment.class));
            verify(internshipService).update(any(Internship.class));
        }

        @Test
        @DisplayName("Should add comment to marketplace item successfully")
        void shouldAddCommentToMarketplaceItem() {
            when(marketplaceService.findById(testItemId)).thenReturn(Optional.of(testItem));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            Comment result = commentsService.addComment(CommentParentType.MARKETPLACE, testItemId,
                    new CreateCommentRequest("Marketplace comment"), testUserId);

            assertNotNull(result);
            verify(commentRepository).save(any(Comment.class));
            verify(marketplaceService).update(any(MarketplaceItem.class));
        }

        @Test
        @DisplayName("Should set profile name on saved comment from user entity")
        void shouldSetProfileNameOnComment() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            when(commentRepository.save(captor.capture())).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Test"), testUserId);

            assertEquals("TestUser", captor.getValue().getProfileName());
        }

        @Test
        @DisplayName("Should increment comment count on parent after saving")
        void shouldIncrementCommentCountOnParent() {
            int initialCount = testPost.getCommentCount();
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Test"), testUserId);

            assertEquals(initialCount + 1, testPost.getCommentCount(),
                    "Comment count on parent must be incremented after save");
            verify(postsService).update(testPost);
        }

        @Test
        @DisplayName("Should publish CommentCreatedEvent for POST parent")
        void shouldPublishCommentCreatedEventForPost() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Test"), testUserId);

            verify(eventPublisher).publishEvent(any(CommentCreatedEvent.class));
            verify(internshipCommentEventPublisher, never()).publishEvent(any());
            verify(marketplaceCommentEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should publish InternshipCommentCreatedEvent for INTERNSHIP parent")
        void shouldPublishInternshipCommentEvent() {
            when(internshipService.findById(testInternshipId)).thenReturn(Optional.of(testInternship));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            commentsService.addComment(CommentParentType.INTERNSHIP, testInternshipId,
                    new CreateCommentRequest("Test"), testUserId);

            verify(internshipCommentEventPublisher).publishEvent(any(InternshipCommentCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any());
            verify(marketplaceCommentEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should publish MarketplaceCommentCreatedEvent for MARKETPLACE parent")
        void shouldPublishMarketplaceCommentEvent() {
            when(marketplaceService.findById(testItemId)).thenReturn(Optional.of(testItem));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            commentsService.addComment(CommentParentType.MARKETPLACE, testItemId,
                    new CreateCommentRequest("Test"), testUserId);

            verify(marketplaceCommentEventPublisher).publishEvent(any(MarketplaceCommentCreatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any());
            verify(internshipCommentEventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should accept comment text of exactly 5000 characters — boundary")
        void shouldAcceptMaxLengthComment() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            assertDoesNotThrow(() -> commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("a".repeat(5000)), testUserId));
        }

        @Test
        @DisplayName("Should handle special characters and Unicode in comment text")
        void shouldHandleSpecialAndUnicodeCharacters() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            assertDoesNotThrow(() -> commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Test 🎉 测试 !@#$%\nLine 2"), testUserId));
        }
    }

    // ─── Add Comment — Validation Errors ──────────────────────────────────────

    @Nested
    @DisplayName("Add Comment — Validation Errors")
    class AddCommentValidationErrors {

        @Test
        @DisplayName("Should throw when post not found")
        void shouldThrowForNonExistentPost() {
            when(postsService.findById(testPostId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("Test"), testUserId));
            assertEquals("Post not found", ex.getMessage());
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should throw when internship not found")
        void shouldThrowForNonExistentInternship() {
            when(internshipService.findById(testInternshipId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.INTERNSHIP, testInternshipId,
                            new CreateCommentRequest("Test"), testUserId));
            assertEquals("Internship not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when marketplace item not found")
        void shouldThrowForNonExistentMarketplaceItem() {
            when(marketplaceService.findById(testItemId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.MARKETPLACE, testItemId,
                            new CreateCommentRequest("Test"), testUserId));
            assertEquals("Marketplace item not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when post is hidden")
        void shouldThrowForHiddenPost() {
            testPost.setHidden(true);
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("Test"), testUserId));
            assertEquals("Content not found", ex.getMessage());
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should throw when comment text is null")
        void shouldThrowForNullText() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest(null), testUserId));
            assertTrue(ex.getMessage().contains("cannot be empty"));
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should throw when comment text is empty")
        void shouldThrowForEmptyText() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest(""), testUserId));
            assertTrue(ex.getMessage().contains("cannot be empty"));
        }

        @Test
        @DisplayName("Should throw when comment text is only whitespace")
        void shouldThrowForWhitespaceOnlyText() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));

            assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("   "), testUserId));
        }

        @Test
        @DisplayName("Should throw when comment text exceeds 5000 characters")
        void shouldThrowForTextTooLong() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("a".repeat(5001)), testUserId));
            assertTrue(ex.getMessage().contains("exceeds maximum length"));
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowForNonExistentUser() {
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("Test"), testUserId));
            assertEquals("User not found", ex.getMessage());
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when user is from different school on campus post")
        void shouldThrowForDifferentSchool() {
            testPost.setWall("campus");
            testPost.setSchoolDomain("mit.edu");
            testUser.setSchoolDomain("harvard.edu");

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("Test"), testUserId));
            assertTrue(ex.getMessage().contains("other schools"));
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should throw when user has no school domain on campus post")
        void shouldThrowWhenUserHasNoSchoolDomain() {
            testPost.setWall("campus");
            testPost.setSchoolDomain("harvard.edu");
            testUser.setSchoolDomain(null);

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("Test"), testUserId));
            assertTrue(ex.getMessage().contains("campus posts"));
        }

        @Test
        @DisplayName("Should throw when user not found during campus visibility check")
        void shouldThrowWhenUserNotFoundDuringCampusCheck() {
            testPost.setWall("campus");
            testPost.setSchoolDomain("harvard.edu");

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            // Campus check calls findById; returning empty should throw here, before profileName lookup
            when(userService.findById(testUserId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.addComment(CommentParentType.POST, testPostId,
                            new CreateCommentRequest("Test"), testUserId));
            assertEquals("User not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should NOT call userService during visibility check for national post")
        void shouldNotCallUserServiceDuringNationalVisibilityCheck() {
            // national wall skips user lookup in validateParentVisibility entirely —
            // userService is only called once (for profileName) not twice.
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            stubSaveReturnsWithId();

            commentsService.addComment(CommentParentType.POST, testPostId,
                    new CreateCommentRequest("Test"), testUserId);

            verify(userService, times(1)).findById(testUserId);
        }
    }

    // ─── Hide Comment — Positive ───────────────────────────────────────────────

    @Nested
    @DisplayName("Hide Comment — Positive Cases")
    class HideCommentPositiveCases {

        @Test
        @DisplayName("Should hide own comment and decrement parent comment count")
        void shouldHideOwnComment() {
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, testUserId);
            comment.setId(commentId);
            int initialCount = testPost.getCommentCount();

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

            Comment result = commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            assertTrue(result.isHidden());
            verify(commentRepository).update(any(Comment.class));
            verify(postsService).update(testPost);
            assertEquals(Math.max(initialCount - 1, 0), testPost.getCommentCount(),
                    "Comment count must be decremented after hiding");
        }

        @Test
        @DisplayName("Should return already-hidden comment without updating or decrementing count")
        void shouldReturnIfAlreadyHidden() {
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, testUserId);
            comment.setId(commentId);
            comment.setHidden(true);
            int initialCount = testPost.getCommentCount();

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            Comment result = commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            assertTrue(result.isHidden());
            // Both update AND saveParent must be skipped — count must not change
            verify(commentRepository, never()).update(any());
            verify(postsService, never()).update(any());
            assertEquals(initialCount, testPost.getCommentCount(),
                    "Comment count must not change when comment was already hidden");
        }
    }

    // ─── Hide Comment — Negative ───────────────────────────────────────────────

    @Nested
    @DisplayName("Hide Comment — Negative Cases")
    class HideCommentNegativeCases {

        @Test
        @DisplayName("Should throw when hiding another user's comment")
        void shouldThrowWhenHidingOthersComment() {
            UUID commentId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, otherUserId);
            comment.setId(commentId);

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertTrue(ex.getMessage().contains("only hide your own"));
            verify(commentRepository, never()).update(any());
            verify(postsService, never()).update(any());
        }

        @Test
        @DisplayName("Should throw when comment not found")
        void shouldThrowForNonExistentComment() {
            UUID commentId = UUID.randomUUID();
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertEquals("Comment not found", ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when comment belongs to a different parent")
        void shouldThrowWhenCommentBelongsToDifferentPost() {
            UUID commentId = UUID.randomUUID();
            UUID otherPostId = UUID.randomUUID();
            Comment comment = buildComment(otherPostId, testUserId);
            comment.setId(commentId);

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.hideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertTrue(ex.getMessage().contains("does not belong"));
        }
    }

    // ─── Unhide Comment — Positive ─────────────────────────────────────────────

    @Nested
    @DisplayName("Unhide Comment — Positive Cases")
    class UnhideCommentPositiveCases {

        @Test
        @DisplayName("Should unhide own comment and increment parent comment count")
        void shouldUnhideOwnComment() {
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, testUserId);
            comment.setId(commentId);
            comment.setHidden(true);
            int initialCount = testPost.getCommentCount();

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

            Comment result = commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            assertFalse(result.isHidden());
            verify(commentRepository).update(any(Comment.class));
            verify(postsService).update(testPost);
            assertEquals(initialCount + 1, testPost.getCommentCount(),
                    "Comment count must be incremented after unhiding");
        }

        @Test
        @DisplayName("Should return already-visible comment without updating or incrementing count")
        void shouldReturnIfNotHidden() {
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, testUserId);
            comment.setId(commentId);
            comment.setHidden(false);
            int initialCount = testPost.getCommentCount();

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            Comment result = commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId);

            assertFalse(result.isHidden());
            // Both update AND saveParent must be skipped — count must not change
            verify(commentRepository, never()).update(any());
            verify(postsService, never()).update(any());
            assertEquals(initialCount, testPost.getCommentCount(),
                    "Comment count must not change when comment was already visible");
        }
    }

    // ─── Unhide Comment — Negative ─────────────────────────────────────────────

    @Nested
    @DisplayName("Unhide Comment — Negative Cases")
    class UnhideCommentNegativeCases {

        @Test
        @DisplayName("Should throw when unhiding another user's comment")
        void shouldThrowWhenUnhidingOthersComment() {
            UUID commentId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, otherUserId);
            comment.setId(commentId);
            comment.setHidden(true);

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId));
            assertTrue(ex.getMessage().contains("only unhide your own"));
            verify(commentRepository, never()).update(any());
            verify(postsService, never()).update(any());
        }

        @Test
        @DisplayName("Should throw when comment not found")
        void shouldThrowForNonExistentComment() {
            UUID commentId = UUID.randomUUID();
            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId));
        }

        @Test
        @DisplayName("Should throw when comment belongs to a different parent")
        void shouldThrowWhenCommentBelongsToDifferentPost() {
            UUID commentId = UUID.randomUUID();
            UUID otherPostId = UUID.randomUUID();
            Comment comment = buildComment(otherPostId, testUserId);
            comment.setId(commentId);
            comment.setHidden(true);

            when(postsService.findById(testPostId)).thenReturn(Optional.of(testPost));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

            assertThrows(IllegalArgumentException.class,
                    () -> commentsService.unhideComment(CommentParentType.POST, testPostId, commentId, testUserId));
        }
    }

    // ─── Get Comments With Pagination ──────────────────────────────────────────

    @Nested
    @DisplayName("Get Comments With Pagination")
    class GetCommentsWithPagination {

        @Test
        @DisplayName("Should return comments ordered by newest when sortBy is NEWEST")
        void shouldReturnCommentsNewest() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable)).thenReturn(mockPage);

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, testPostId, pageable, SortBy.NEWEST);

            assertSame(mockPage, result);
            verify(commentRepository).findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable);
        }

        @Test
        @DisplayName("Should return comments ordered by oldest when sortBy is OLDEST")
        void shouldReturnCommentsOldest() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtAsc(
                    "POST", testPostId, pageable)).thenReturn(mockPage);

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, testPostId, pageable, SortBy.OLDEST);

            assertSame(mockPage, result);
            verify(commentRepository).findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtAsc(
                    "POST", testPostId, pageable);
        }

        @Test
        @DisplayName("Should default to NEWEST order when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable)).thenReturn(mockPage);

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, testPostId, pageable, null);

            assertSame(mockPage, result);
            verify(commentRepository).findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable);
        }

        @Test
        @DisplayName("Should filter out comments from blocked users when currentUserId provided")
        void shouldFilterBlockedUserComments() {
            UUID blockedUserId = UUID.randomUUID();
            Pageable pageable = Pageable.from(0, 20);

            Comment visibleComment = buildComment(testPostId, testUserId);
            Comment blockedComment = buildComment(testPostId, blockedUserId);

            Page<Comment> mockPage = mock(Page.class);
            when(mockPage.getContent()).thenReturn(List.of(visibleComment, blockedComment));
            when(mockPage.getPageable()).thenReturn(pageable);
            when(mockPage.getTotalSize()).thenReturn(2L);

            when(commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable)).thenReturn(mockPage);
            when(userBlockService.getCombinedBlockedUserIds(testUserId))
                    .thenReturn(Set.of(blockedUserId));

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, testPostId, pageable, SortBy.NEWEST, testUserId);

            assertEquals(1, result.getContent().size());
            assertEquals(testUserId, result.getContent().get(0).getUserId());
            assertEquals(1L, result.getTotalSize(),
                    "Total size must be reduced by number of filtered comments");
        }

        @Test
        @DisplayName("Should return all comments unfiltered when currentUserId is null")
        void shouldReturnAllCommentsWhenCurrentUserIdNull() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable)).thenReturn(mockPage);

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, testPostId, pageable, SortBy.NEWEST, null);

            assertSame(mockPage, result);
            verifyNoInteractions(userBlockService);
        }

        @Test
        @DisplayName("Should return all comments unfiltered when blocked set is empty")
        void shouldReturnAllCommentsWhenNoBlocksExist() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByParentTypeAndParentIdAndHiddenFalseOrderByCreatedAtDesc(
                    "POST", testPostId, pageable)).thenReturn(mockPage);
            when(userBlockService.getCombinedBlockedUserIds(testUserId))
                    .thenReturn(Collections.emptySet());

            Page<Comment> result = commentsService.getCommentsWithPagination(
                    CommentParentType.POST, testPostId, pageable, SortBy.NEWEST, testUserId);

            // Impl returns early without building a new Page when blockedUserIds is empty
            assertSame(mockPage, result);
        }
    }

    // ─── Get User Own Comments ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Get User Own Comments")
    class GetUserOwnComments {

        @Test
        @DisplayName("Should return user comments ordered by newest when sortBy is NEWEST")
        void shouldReturnUserCommentsNewest() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(testUserId, pageable))
                    .thenReturn(mockPage);

            Page<Comment> result = commentsService.getUserOwnComments(testUserId, pageable, SortBy.NEWEST);

            assertSame(mockPage, result);
            verify(commentRepository).findByUserIdAndHiddenFalseOrderByCreatedAtDesc(testUserId, pageable);
        }

        @Test
        @DisplayName("Should return user comments ordered by oldest when sortBy is OLDEST")
        void shouldReturnUserCommentsOldest() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtAsc(testUserId, pageable))
                    .thenReturn(mockPage);

            Page<Comment> result = commentsService.getUserOwnComments(testUserId, pageable, SortBy.OLDEST);

            assertSame(mockPage, result);
            verify(commentRepository).findByUserIdAndHiddenFalseOrderByCreatedAtAsc(testUserId, pageable);
        }

        @Test
        @DisplayName("Should default to NEWEST order when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            Pageable pageable = Pageable.from(0, 20);
            Page<Comment> mockPage = mock(Page.class);
            when(commentRepository.findByUserIdAndHiddenFalseOrderByCreatedAtDesc(testUserId, pageable))
                    .thenReturn(mockPage);

            Page<Comment> result = commentsService.getUserOwnComments(testUserId, pageable, null);

            assertSame(mockPage, result);
            verify(commentRepository).findByUserIdAndHiddenFalseOrderByCreatedAtDesc(testUserId, pageable);
        }
    }

    // ─── Report Comment ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Report Comment")
    class ReportComment {

        @Test
        @DisplayName("Should create report and increment author report count")
        void shouldCreateReportAndIncrementCount() {
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, UUID.randomUUID()); // author != reporter
            comment.setId(commentId);
            UserEntity author = new UserEntity();
            author.setId(comment.getUserId());
            author.setReportCount(2);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReportService.existsByCommentIdAndReporterUserId(commentId, testUserId))
                    .thenReturn(false);
            when(userService.findById(comment.getUserId())).thenReturn(Optional.of(author));

            commentsService.reportComment(commentId, testUserId, "Spam");

            verify(commentReportService).save(any(CommentReport.class));
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userService).update(captor.capture());
            assertEquals(3, captor.getValue().getReportCount(),
                    "Author report count must be incremented by 1");
        }

        @Test
        @DisplayName("Should set correct fields on saved CommentReport")
        void shouldSetCorrectFieldsOnReport() {
            UUID commentId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            Comment comment = new Comment(testPostId, "POST", authorId, "Text");
            comment.setId(commentId);
            UserEntity author = new UserEntity();
            author.setId(authorId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReportService.existsByCommentIdAndReporterUserId(commentId, testUserId))
                    .thenReturn(false);
            when(userService.findById(authorId)).thenReturn(Optional.of(author));

            commentsService.reportComment(commentId, testUserId, "Offensive");

            ArgumentCaptor<CommentReport> captor = ArgumentCaptor.forClass(CommentReport.class);
            verify(commentReportService).save(captor.capture());
            CommentReport saved = captor.getValue();
            assertEquals(commentId, saved.getCommentId());
            assertEquals(testUserId, saved.getReporterUserId());
            assertEquals(authorId, saved.getReportedUserId(),
                    "reportedUserId must be the comment author, not the reporter");
            assertEquals("Offensive", saved.getReason());
        }

        @Test
        @DisplayName("Should throw when comment not found")
        void shouldThrowWhenCommentNotFound() {
            UUID commentId = UUID.randomUUID();
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.reportComment(commentId, testUserId, "Reason"));
            assertEquals("Comment not found", ex.getMessage());
            verifyNoInteractions(commentReportService);
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should throw when user has already reported this comment")
        void shouldThrowOnDuplicateReport() {
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, UUID.randomUUID());
            comment.setId(commentId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReportService.existsByCommentIdAndReporterUserId(commentId, testUserId))
                    .thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> commentsService.reportComment(commentId, testUserId, "Duplicate"));
            assertEquals("You have already reported this comment", ex.getMessage());
            verify(commentReportService, never()).save(any());
            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("Should not throw when author not found — report is still saved")
        void shouldStillSaveReportWhenAuthorNotFound() {
            // Impl checks authorOpt.isPresent() before incrementing — gracefully skips if not found.
            UUID commentId = UUID.randomUUID();
            Comment comment = buildComment(testPostId, UUID.randomUUID());
            comment.setId(commentId);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentReportService.existsByCommentIdAndReporterUserId(commentId, testUserId))
                    .thenReturn(false);
            when(userService.findById(comment.getUserId())).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> commentsService.reportComment(commentId, testUserId, "Reason"));
            verify(commentReportService).save(any(CommentReport.class));
            verify(userService, never()).update(any());
        }
    }

    // ─── Update Profile Name ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Update Profile Name By UserId")
    class UpdateProfileName {

        @Test
        @DisplayName("Should delegate to repository and succeed")
        void shouldUpdateProfileName() {
            doNothing().when(commentRepository).updateProfileNameByUserId(testUserId, "NewName");

            assertDoesNotThrow(() ->
                    commentsService.updateProfileNameByUserId(testUserId, "NewName"));

            verify(commentRepository).updateProfileNameByUserId(testUserId, "NewName");
        }

        @Test
        @DisplayName("Should rethrow exception from repository — @Retryable sees it")
        void shouldRethrowRepositoryException() {
            RuntimeException dbError = new RuntimeException("DB error");
            doThrow(dbError).when(commentRepository).updateProfileNameByUserId(testUserId, "NewName");

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> commentsService.updateProfileNameByUserId(testUserId, "NewName"));
            assertSame(dbError, thrown,
                    "Exception must be rethrown as-is so @Retryable can intercept it");
        }
    }
}
