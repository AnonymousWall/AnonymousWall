package com.anonymous.wall.service.base;

import com.anonymous.wall.entity.CommentReport;

import java.util.UUID;

public interface CommentReportService {
    boolean existsByCommentIdAndReporterUserId(UUID commentId, UUID reporterUserId);
    void save(CommentReport report);
}
