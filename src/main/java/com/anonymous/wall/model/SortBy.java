package com.anonymous.wall.model;

/**
 * Enumeration for sorting options
 */
public enum SortBy {
    /**
     * Sort by creation time, newest first (default)
     */
    NEWEST("created_at", "DESC"),

    /**
     * Sort by creation time, oldest first
     */
    OLDEST("created_at", "ASC"),

    /**
     * Sort by most likes, highest first
     */
    MOST_LIKED("like_count", "DESC"),

    /**
     * Sort by least likes, lowest first
     */
    LEAST_LIKED("like_count", "ASC");

    private final String column;
    private final String direction;

    SortBy(String column, String direction) {
        this.column = column;
        this.direction = direction;
    }

    public String getColumn() {
        return column;
    }

    public String getDirection() {
        return direction;
    }

    /**
     * Parse string to SortBy enum (case-insensitive)
     * Defaults to NEWEST if invalid
     */
    public static SortBy parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NEWEST;
        }
        try {
            return SortBy.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEWEST;
        }
    }
}
