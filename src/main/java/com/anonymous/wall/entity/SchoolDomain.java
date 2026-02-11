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

    @MappedProperty("domain")
    private String domain;

    @MappedProperty("school_name")
    private String schoolName;

    @MappedProperty("created_at")
    private ZonedDateTime createdAt;

    // ---------------- Getters & Setters ----------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
