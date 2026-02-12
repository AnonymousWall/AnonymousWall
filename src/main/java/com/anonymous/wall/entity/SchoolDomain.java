package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "school_domains", namingStrategy = NamingStrategies.Raw.class)
public class SchoolDomain {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("domain")
    private String domain;

    @MappedProperty("school_name")
    private String schoolName;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt;

    // ---------------- Getters & Setters ----------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
