package com.anonymous.wall.notification.event;

import java.util.UUID;

public class MarketplaceCommentCreatedEvent {
    private final UUID commentId;
    private final UUID itemId;
    private final UUID actorUserId;
    private final UUID itemOwnerId;

    public MarketplaceCommentCreatedEvent(UUID commentId, UUID itemId, UUID actorUserId, UUID itemOwnerId) {
        this.commentId = commentId;
        this.itemId = itemId;
        this.actorUserId = actorUserId;
        this.itemOwnerId = itemOwnerId;
    }

    public UUID getCommentId() { return commentId; }
    public UUID getItemId() { return itemId; }
    public UUID getActorUserId() { return actorUserId; }
    public UUID getItemOwnerId() { return itemOwnerId; }
}
