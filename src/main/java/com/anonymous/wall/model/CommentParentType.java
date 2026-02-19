package com.anonymous.wall.model;

/**
 * Enum representing the type of entity that a comment can be attached to.
 * Used for the polymorphic comment system where comments can belong to
 * posts, internships, or marketplace items.
 */
public enum CommentParentType {
    POST,
    INTERNSHIP,
    MARKETPLACE;

    /**
     * Parse a string to CommentParentType (case-insensitive).
     */
    public static CommentParentType parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Parent type cannot be null");
        }
        try {
            return CommentParentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid parent type: " + value + ". Must be one of: POST, INTERNSHIP, MARKETPLACE");
        }
    }
}
