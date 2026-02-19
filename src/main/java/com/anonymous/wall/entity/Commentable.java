package com.anonymous.wall.entity;

import java.util.UUID;

/**
 * Interface for entities that can have comments attached to them.
 * Implemented by Post, Internship, and MarketplaceItem.
 */
public interface Commentable {

    UUID getId();

    UUID getUserId();

    boolean isHidden();

    String getWall();

    String getSchoolDomain();

    int getCommentCount();

    void incrementCommentCount();

    void decrementCommentCount();
}
