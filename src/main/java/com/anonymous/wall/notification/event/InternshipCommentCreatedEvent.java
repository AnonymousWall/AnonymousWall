package com.anonymous.wall.notification.event;

import java.util.UUID;

public class InternshipCommentCreatedEvent {
    private final UUID commentId;
    private final UUID internshipId;
    private final UUID actorUserId;
    private final UUID internshipOwnerId;

    public InternshipCommentCreatedEvent(UUID commentId, UUID internshipId, UUID actorUserId, UUID internshipOwnerId) {
        this.commentId = commentId;
        this.internshipId = internshipId;
        this.actorUserId = actorUserId;
        this.internshipOwnerId = internshipOwnerId;
    }

    public UUID getCommentId() { return commentId; }
    public UUID getInternshipId() { return internshipId; }
    public UUID getActorUserId() { return actorUserId; }
    public UUID getInternshipOwnerId() { return internshipOwnerId; }
}
