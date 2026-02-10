package com.anonymous.wall.event;

import java.util.UUID;

/**
 * Event fired when a post is hidden.
 * This event triggers asynchronous updates to hide all comments associated with the post.
 */
public class PostHiddenEvent {
    
    private final UUID postId;
    private final UUID userId;
    
    public PostHiddenEvent(UUID postId, UUID userId) {
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
        return "PostHiddenEvent{" +
                "postId=" + postId +
                ", userId=" + userId +
                '}';
    }
}
