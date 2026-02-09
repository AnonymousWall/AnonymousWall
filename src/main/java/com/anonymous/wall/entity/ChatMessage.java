package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "chat_messages", namingStrategy = NamingStrategies.Raw.class)
public class ChatMessage {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("room_id")
    private UUID roomId;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("profile_name")
    private String profileName = "Anonymous";

    @MappedProperty("content")
    private String content;

    @MappedProperty("is_deleted")
    private boolean deleted = false;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    // Constructors
    public ChatMessage() {}

    public ChatMessage(UUID roomId, UUID userId, String profileName, String content) {
        this.roomId = roomId;
        this.userId = userId;
        this.profileName = profileName;
        this.content = content;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRoomId() {
        return roomId;
    }

    public void setRoomId(UUID roomId) {
        this.roomId = roomId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
