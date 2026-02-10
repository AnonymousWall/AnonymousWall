package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "chat_rooms", namingStrategy = NamingStrategies.Raw.class)
public class ChatRoom {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("created_by")
    private UUID createdBy;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @MappedProperty("updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    // Constructors
    public ChatRoom() {}

    public ChatRoom(UUID createdBy) {
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
