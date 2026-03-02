package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "notifications", namingStrategy = NamingStrategies.Raw.class)
public class NotificationEntity {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("recipient_user_id")
    private UUID recipientUserId;

    @MappedProperty("actor_user_id")
    private UUID actorUserId;

    @MappedProperty("type")
    private String type;

    @MappedProperty("entity_id")
    private UUID entityId;

    @MappedProperty("entity_title")
    private String entityTitle;

    @MappedProperty("actor_profile_name")
    private String actorProfileName;

    @MappedProperty("read")
    private boolean read = false;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ================= Constructors =================

    public NotificationEntity() {
    }

    public NotificationEntity(UUID recipientUserId, UUID actorUserId, String type, UUID entityId,
                               String entityTitle, String actorProfileName) {
        this.recipientUserId = recipientUserId;
        this.actorUserId = actorUserId;
        this.type = type;
        this.entityId = entityId;
        this.entityTitle = entityTitle;
        this.actorProfileName = actorProfileName;
        this.read = false;
        this.createdAt = OffsetDateTime.now();
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }

    public UUID getActorUserId() { return actorUserId; }
    public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }

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

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
