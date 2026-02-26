package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "user_blocks", namingStrategy = NamingStrategies.Raw.class)
public class UserBlock {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("blocker_id")
    private UUID blockerId;

    @MappedProperty("blocked_id")
    private UUID blockedId;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ================= Constructors =================

    public UserBlock() {
    }

    public UserBlock(UUID blockerId, UUID blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = OffsetDateTime.now();
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBlockerId() { return blockerId; }
    public void setBlockerId(UUID blockerId) { this.blockerId = blockerId; }

    public UUID getBlockedId() { return blockedId; }
    public void setBlockedId(UUID blockedId) { this.blockedId = blockedId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
