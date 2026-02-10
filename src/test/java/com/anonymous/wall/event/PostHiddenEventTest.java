package com.anonymous.wall.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PostHiddenEvent Tests")
class PostHiddenEventTest {

    @Test
    @DisplayName("Should create event with all fields")
    void shouldCreateEventWithAllFields() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        // Act
        PostHiddenEvent event = new PostHiddenEvent(postId, userId);
        
        // Assert
        assertNotNull(event);
        assertEquals(postId, event.getPostId());
        assertEquals(userId, event.getUserId());
    }
    
    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PostHiddenEvent event = new PostHiddenEvent(postId, userId);
        
        // Act
        String eventString = event.toString();
        
        // Assert
        assertNotNull(eventString);
        assertTrue(eventString.contains("PostHiddenEvent"));
        assertTrue(eventString.contains(postId.toString()));
        assertTrue(eventString.contains(userId.toString()));
    }
}
