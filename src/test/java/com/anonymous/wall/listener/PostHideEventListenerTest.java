package com.anonymous.wall.listener;

import com.anonymous.wall.event.PostHiddenEvent;
import com.anonymous.wall.event.PostUnhiddenEvent;
import com.anonymous.wall.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PostHideEventListener Tests")
class PostHideEventListenerTest {

    private PostHideEventListener listener;
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        listener = new PostHideEventListener();
        
        // Use reflection to inject mock
        try {
            var commentRepoField = PostHideEventListener.class.getDeclaredField("commentRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentRepository);
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
        verify(commentRepository, times(1)).updateByPostId(postId, true);
    }
    
    @Test
    @DisplayName("Should unhide comments when PostUnhiddenEvent is received")
    void shouldUnhideCommentsWhenPostUnhiddenEventReceived() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PostUnhiddenEvent event = new PostUnhiddenEvent(postId, userId);
        
        // Act
        listener.onApplicationEvent(event);
        
        // Assert
        verify(commentRepository, times(1)).updateByPostId(postId, false);
    }
    
    @Test
    @DisplayName("Should handle repository exceptions gracefully for hide")
    void shouldHandleRepositoryExceptionsGracefullyForHide() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PostHiddenEvent event = new PostHiddenEvent(postId, userId);
        
        doThrow(new RuntimeException("Database error"))
            .when(commentRepository).updateByPostId(any(), anyBoolean());
        
        // Act - should not throw exception
        listener.onApplicationEvent(event);
        
        // Assert
        verify(commentRepository, times(1)).updateByPostId(postId, true);
    }
    
    @Test
    @DisplayName("Should handle repository exceptions gracefully for unhide")
    void shouldHandleRepositoryExceptionsGracefullyForUnhide() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PostUnhiddenEvent event = new PostUnhiddenEvent(postId, userId);
        
        doThrow(new RuntimeException("Database error"))
            .when(commentRepository).updateByPostId(any(), anyBoolean());
        
        // Act - should not throw exception
        listener.onApplicationEvent(event);
        
        // Assert
        verify(commentRepository, times(1)).updateByPostId(postId, false);
    }
    
    @Test
    @DisplayName("Should support PostHiddenEvent")
    void shouldSupportPostHiddenEvent() {
        // Arrange
        PostHiddenEvent event = new PostHiddenEvent(UUID.randomUUID(), UUID.randomUUID());
        
        // Act & Assert
        assertTrue(listener.supports(event));
    }
    
    @Test
    @DisplayName("Should support PostUnhiddenEvent")
    void shouldSupportPostUnhiddenEvent() {
        // Arrange
        PostUnhiddenEvent event = new PostUnhiddenEvent(UUID.randomUUID(), UUID.randomUUID());
        
        // Act & Assert
        assertTrue(listener.supports(event));
    }
    
    @Test
    @DisplayName("Should not support other events")
    void shouldNotSupportOtherEvents() {
        // Arrange
        Object event = new Object();
        
        // Act & Assert
        assertFalse(listener.supports(event));
    }
}
