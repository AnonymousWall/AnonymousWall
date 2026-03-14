package com.anonymous.wall.listener;

import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.listener.helper.CommentHideTransactionHelper;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.service.base.CommentsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("PostHideEventListener Tests")
class PostHideEventListenerTest {

    private PostHideEventListener listener;
    private CommentHideTransactionHelper commentHideTransactionHelper;

    @BeforeEach
    void setUp() {
        commentHideTransactionHelper = mock(CommentHideTransactionHelper.class);
        listener = new PostHideEventListener();
        
        // Use reflection to inject mock
        try {
            var commentRepoField = PostHideEventListener.class.getDeclaredField("commentHideTransactionHelper");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentHideTransactionHelper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should hide comments when PostHiddenEvent is received")
    void shouldHideCommentsWhenPostHiddenEventReceived() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PostHiddenEvent event = new PostHiddenEvent(postId, userId);
        
        // Act
        listener.onApplicationEvent(event);
        
        // Assert
        verify(commentHideTransactionHelper, times(1)).hideCommentsByParent("POST", postId, true);
    }
    
    @Test
    @DisplayName("Should handle service exceptions gracefully for hide")
    void shouldHandleServiceExceptionsGracefullyForHide() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PostHiddenEvent event = new PostHiddenEvent(postId, userId);
        
        doThrow(new RuntimeException("Database error"))
            .when(commentHideTransactionHelper).hideCommentsByParent(any(), any(), anyBoolean());
        
        // Act - should not throw exception
        listener.onApplicationEvent(event);
        
        // Assert
        verify(commentHideTransactionHelper, times(1)).hideCommentsByParent("POST", postId, true);
    }
}
