package com.anonymous.wall.service.impl;

import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.service.base.CommentReportService;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class CommentReportServiceImpl implements CommentReportService {

    @Inject
    private CommentReportRepository commentReportRepository;

    @Override
    @Transactional
    public boolean existsByCommentIdAndReporterUserId(UUID commentId, UUID reporterUserId) {
        return commentReportRepository.existsByCommentIdAndReporterUserId(commentId, reporterUserId);
    }

    @Override
    @Transactional
    public void save(CommentReport report) {
        commentReportRepository.save(report);
    }
}
