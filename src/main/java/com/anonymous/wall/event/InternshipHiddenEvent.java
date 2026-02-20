package com.anonymous.wall.event;

import java.util.UUID;

/**
 * Event fired when an internship posting is hidden.
 * This event triggers asynchronous updates to hide all comments associated with the internship.
 */
public class InternshipHiddenEvent {

    private final UUID internshipId;
    private final UUID userId;

    public InternshipHiddenEvent(UUID internshipId, UUID userId) {
        this.internshipId = internshipId;
        this.userId = userId;
    }

    public UUID getInternshipId() {
        return internshipId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "InternshipHiddenEvent{" +
                "internshipId=" + internshipId +
                ", userId=" + userId +
                '}';
    }
}
