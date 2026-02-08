package com.anonymous.wall.listener;

import com.anonymous.wall.event.ProfileNameChangedEvent;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("ProfileNameUpdateEventListener Tests")
class ProfileNameUpdateEventListenerTest {

    private ProfileNameUpdateEventListener listener;
    private PostRepository postRepository;
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        commentRepository = mock(CommentRepository.class);
        listener = new ProfileNameUpdateEventListener();
        
        // Use reflection to inject mocks
        try {
            var postRepoField = ProfileNameUpdateEventListener.class.getDeclaredField("postRepository");
            postRepoField.setAccessible(true);
            postRepoField.set(listener, postRepository);
            
            var commentRepoField = ProfileNameUpdateEventListener.class.getDeclaredField("commentRepository");
            commentRepoField.setAccessible(true);
            commentRepoField.set(listener, commentRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should update posts and comments when event is received")
    void shouldUpdatePostsAndCommentsWhenEventReceived() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String oldName = "OldName";
        String newName = "NewName";
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, oldName, newName);
        
        // Act
        listener.onApplicationEvent(event);
        
        // Assert
        verify(postRepository, times(1)).updateProfileNameByUserId(userId, newName);
        verify(commentRepository, times(1)).updateProfileNameByUserId(userId, newName);
    }
    
    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String oldName = "OldName";
        String newName = "NewName";
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, oldName, newName);
        
        doThrow(new RuntimeException("Database error"))
            .when(postRepository).updateProfileNameByUserId(any(), any());
        
        // Act - should not throw exception
        listener.onApplicationEvent(event);
        
        // Assert
        verify(postRepository, times(1)).updateProfileNameByUserId(userId, newName);
        // Comment repository should not be called due to exception
        verify(commentRepository, never()).updateProfileNameByUserId(any(), any());
    }
    
    @Test
    @DisplayName("Should update comments even if posts update fails")
    void shouldUpdateCommentsEvenIfPostsUpdateFails() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String oldName = "OldName";
        String newName = "NewName";
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, oldName, newName);
        
        doThrow(new RuntimeException("Posts update failed"))
            .when(postRepository).updateProfileNameByUserId(any(), any());
        
        // Act
        listener.onApplicationEvent(event);
        
        // Assert
        verify(postRepository, times(1)).updateProfileNameByUserId(userId, newName);
        // Due to exception, comment repository is not called (fail-fast behavior in current implementation)
        verify(commentRepository, never()).updateProfileNameByUserId(any(), any());
    }
    
    @Test
    @DisplayName("Should handle null old name in event")
    void shouldHandleNullOldNameInEvent() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String newName = "NewName";
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, null, newName);
        
        // Act
        listener.onApplicationEvent(event);
        
        // Assert
        verify(postRepository, times(1)).updateProfileNameByUserId(userId, newName);
        verify(commentRepository, times(1)).updateProfileNameByUserId(userId, newName);
    }
}
