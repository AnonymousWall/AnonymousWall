package com.anonymous.wall.event;

import java.util.UUID;

/**
 * Event fired when a post is unhidden.
 * This event triggers asynchronous updates to unhide all comments associated with the post.
 */
public class PostUnhiddenEvent {
    
    private final UUID postId;
    private final UUID userId;
    
    public PostUnhiddenEvent(UUID postId, UUID userId) {
        this.postId = postId;
        this.userId = userId;
    }
    
    public UUID getPostId() {
        return postId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    @Override
    public String toString() {
        return "PostUnhiddenEvent{" +
                "postId=" + postId +
                ", userId=" + userId +
                '}';
    }
}
