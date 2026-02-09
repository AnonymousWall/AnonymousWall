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

    @MappedProperty("name")
    private String name;

    @MappedProperty("type")
    private String type; // DIRECT, GROUP, CAMPUS, NATIONAL

    @MappedProperty("school_domain")
    private String schoolDomain;

    @MappedProperty("created_by")
    private UUID createdBy;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @MappedProperty("updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    // Constructors
    public ChatRoom() {}

    public ChatRoom(String name, String type, String schoolDomain, UUID createdBy) {
        this.name = name;
        this.type = type;
        this.schoolDomain = schoolDomain;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSchoolDomain() {
        return schoolDomain;
    }

    public void setSchoolDomain(String schoolDomain) {
        this.schoolDomain = schoolDomain;
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
