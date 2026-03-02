package com.anonymous.wall.notification.event;

import java.util.UUID;

public class CommentCreatedEvent {
    private final UUID commentId;
    private final UUID postId;
    private final UUID actorUserId;
    private final UUID postOwnerId;
    private final String wall;

    public CommentCreatedEvent(UUID commentId, UUID postId, UUID actorUserId, UUID postOwnerId, String wall) {
        this.commentId = commentId;
        this.postId = postId;
        this.actorUserId = actorUserId;
        this.postOwnerId = postOwnerId;
        this.wall = wall;
    }

    public UUID getCommentId() { return commentId; }
    public UUID getPostId() { return postId; }
    public UUID getActorUserId() { return actorUserId; }
    public UUID getPostOwnerId() { return postOwnerId; }
    public String getWall() { return wall; }
}
