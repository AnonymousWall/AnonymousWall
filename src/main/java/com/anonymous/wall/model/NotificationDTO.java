package com.anonymous.wall.model;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public class NotificationDTO {

    private UUID id;
    private String type;
    private UUID entityId;
    private String entityTitle;
    private String actorProfileName;
    private boolean read;
    private String createdAt;

    public NotificationDTO() {
    }

    public NotificationDTO(UUID id, String type, UUID entityId, String entityTitle,
                            String actorProfileName, boolean read, String createdAt) {
        this.id = id;
        this.type = type;
        this.entityId = entityId;
        this.entityTitle = entityTitle;
        this.actorProfileName = actorProfileName;
        this.read = read;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }

    public String getEntityTitle() { return entityTitle; }
    public void setEntityTitle(String entityTitle) { this.entityTitle = entityTitle; }

    public String getActorProfileName() { return actorProfileName; }
    public void setActorProfileName(String actorProfileName) { this.actorProfileName = actorProfileName; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
