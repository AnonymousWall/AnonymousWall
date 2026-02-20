package com.anonymous.wall.admin.service;

import com.anonymous.wall.entity.CommentReport;
import com.anonymous.wall.entity.PostReport;
import com.anonymous.wall.repository.CommentReportRepository;
import com.anonymous.wall.repository.PostReportRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Implementation of admin report management service
 */
@Singleton
public class AdminReportServiceImpl implements AdminReportService {
    
    private static final Logger log = LoggerFactory.getLogger(AdminReportServiceImpl.class);
    
    @Inject
    private PostReportRepository postReportRepository;
    
    @Inject
    private CommentReportRepository commentReportRepository;
    
    @Override
    public Page<PostReport> getAllPostReports(Pageable pageable) {
        log.info("Admin fetching all post reports with pagination: page={}, size={}", 
                 pageable.getNumber(), pageable.getSize());
        return postReportRepository.findAll(pageable);
    }
    
    @Override
    public Page<CommentReport> getAllCommentReports(Pageable pageable) {
        log.info("Admin fetching all comment reports with pagination: page={}, size={}", 
                 pageable.getNumber(), pageable.getSize());
        return commentReportRepository.findAll(pageable);
    }

    @Override
    public PostReport getPostReportById(UUID id) {
        log.info("Admin fetching post report by id: {}", id);
        return postReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post report not found with ID: " + id));
    }

    @Override
    public CommentReport getCommentReportById(UUID id) {
        log.info("Admin fetching comment report by id: {}", id);
        return commentReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment report not found with ID: " + id));
    }

    @Override
    public void resolvePostReport(UUID id) {
        log.info("Admin resolving post report: {}", id);
        PostReport report = postReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post report not found with ID: " + id));
        report.setStatus("RESOLVED");
        postReportRepository.update(report);
    }

    @Override
    public void resolveCommentReport(UUID id) {
        log.info("Admin resolving comment report: {}", id);
        CommentReport report = commentReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment report not found with ID: " + id));
        report.setStatus("RESOLVED");
        commentReportRepository.update(report);
    }

    @Override
    public void rejectPostReport(UUID id) {
        log.info("Admin rejecting post report: {}", id);
        PostReport report = postReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post report not found with ID: " + id));
        report.setStatus("REJECTED");
        postReportRepository.update(report);
    }

    @Override
    public void rejectCommentReport(UUID id) {
        log.info("Admin rejecting comment report: {}", id);
        CommentReport report = commentReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comment report not found with ID: " + id));
        report.setStatus("REJECTED");
        commentReportRepository.update(report);
    }
}
