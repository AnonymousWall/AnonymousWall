package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a chat message in the one-to-one chat system.
 */
@MappedEntity(value = "chat_messages", namingStrategy = NamingStrategies.Raw.class)
public class ChatMessage {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("sender_id")
    private UUID senderId;

    @MappedProperty("receiver_id")
    private UUID receiverId;

    @MappedProperty("conversation_id")
    private UUID conversationId;

    @MappedProperty("content")
    private String content;

    @MappedProperty("read_status")
    private boolean readStatus = false;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ---------------- Constructors ----------------

    public ChatMessage() {}

    public ChatMessage(UUID senderId, UUID receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.readStatus = false;
        this.createdAt = OffsetDateTime.now();
    }

    public ChatMessage(UUID senderId, UUID receiverId, UUID conversationId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.conversationId = conversationId;
        this.content = content;
        this.readStatus = false;
        this.createdAt = OffsetDateTime.now();
    }

    // ---------------- Getters & Setters ----------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
