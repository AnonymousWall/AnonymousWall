package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@MappedEntity(value = "internships", namingStrategy = NamingStrategies.Raw.class)
public class Internship {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("user_id")
    private UUID userId;

    @MappedProperty("company")
    private String company;

    @MappedProperty("role")
    private String role;

    @MappedProperty("salary")
    private String salary;

    @MappedProperty("location")
    private String location;

    @MappedProperty("description")
    private String description;

    @MappedProperty("deadline")
    private LocalDate deadline;

    @MappedProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @MappedProperty("updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ================= Constructors =================

    public Internship() {
    }

    public Internship(UUID userId, String company, String role, String salary, String location, String description, LocalDate deadline) {
        this.userId = userId;
        this.company = company;
        this.role = role;
        this.salary = salary;
        this.location = location;
        this.description = description;
        this.deadline = deadline;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // ================= Getters & Setters =================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
