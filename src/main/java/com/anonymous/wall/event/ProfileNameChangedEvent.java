package com.anonymous.wall.event;

import java.util.UUID;

/**
 * Event fired when a user's profile name changes.
 * This event triggers asynchronous updates to all posts and comments by the user.
 */
public class ProfileNameChangedEvent {
    
    private final UUID userId;
    private final String oldName;
    private final String newName;
    
    public ProfileNameChangedEvent(UUID userId, String oldName, String newName) {
        this.userId = userId;
        this.oldName = oldName;
        this.newName = newName;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getOldName() {
        return oldName;
    }
    
    public String getNewName() {
        return newName;
    }
    
    @Override
    public String toString() {
        return "ProfileNameChangedEvent{" +
                "userId=" + userId +
                ", oldName='" + oldName + '\'' +
                ", newName='" + newName + '\'' +
                '}';
    }
}
