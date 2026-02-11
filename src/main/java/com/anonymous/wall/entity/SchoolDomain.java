package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.ZonedDateTime;
import java.util.UUID;

@MappedEntity(value = "school_domains", namingStrategy = NamingStrategies.Raw.class)
public class SchoolDomain {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("school_id")
    private UUID schoolId;

    @MappedProperty("domain")
    private String domain;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt;

    // ---------------- Getters & Setters ----------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSchoolId() { return schoolId; }
    public void setSchoolId(UUID schoolId) { this.schoolId = schoolId; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
