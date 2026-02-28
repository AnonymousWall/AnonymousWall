package com.anonymous.wall.entity;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.naming.NamingStrategies;

import java.util.UUID;

@MappedEntity(value = "poll_options", namingStrategy = NamingStrategies.Raw.class)
public class PollOption {

    @Id
    @AutoPopulated
    private UUID id;

    @MappedProperty("post_id")
    private UUID postId;

    @MappedProperty("option_text")
    private String optionText;

    @MappedProperty("vote_count")
    private int voteCount = 0;

    @MappedProperty("display_order")
    private int displayOrder = 0;

    // ================= Constructors =================

    public PollOption() {
    }

    public PollOption(UUID postId, String optionText, int displayOrder) {
        this.postId = postId;
        this.optionText = optionText;
        this.displayOrder = displayOrder;
        this.voteCount = 0;
    }

    // ================= Getters & Setters =================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public void incrementVoteCount() { this.voteCount++; }
}
