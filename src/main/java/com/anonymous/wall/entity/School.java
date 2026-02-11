package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "schools", namingStrategy = NamingStrategies.Raw.class)
public class School {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("name")
    private String name;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt;

    // ---------------- Getters & Setters ----------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
