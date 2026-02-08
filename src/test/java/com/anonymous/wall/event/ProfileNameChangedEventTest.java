package com.anonymous.wall.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProfileNameChangedEvent Tests")
class ProfileNameChangedEventTest {

    @Test
    @DisplayName("Should create event with all fields")
    void shouldCreateEventWithAllFields() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String oldName = "OldName";
        String newName = "NewName";
        
        // Act
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, oldName, newName);
        
        // Assert
        assertNotNull(event);
        assertEquals(userId, event.getUserId());
        assertEquals(oldName, event.getOldName());
        assertEquals(newName, event.getNewName());
    }
    
    @Test
    @DisplayName("Should handle null old name")
    void shouldHandleNullOldName() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String newName = "NewName";
        
        // Act
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, null, newName);
        
        // Assert
        assertNotNull(event);
        assertNull(event.getOldName());
        assertEquals(newName, event.getNewName());
    }
    
    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String oldName = "OldName";
        String newName = "NewName";
        ProfileNameChangedEvent event = new ProfileNameChangedEvent(userId, oldName, newName);
        
        // Act
        String eventString = event.toString();
        
        // Assert
        assertNotNull(eventString);
        assertTrue(eventString.contains("ProfileNameChangedEvent"));
        assertTrue(eventString.contains(userId.toString()));
        assertTrue(eventString.contains(oldName));
        assertTrue(eventString.contains(newName));
    }
}
