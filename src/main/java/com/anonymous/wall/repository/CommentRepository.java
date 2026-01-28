package com.anonymous.wall.repository;

import com.anonymous.wall.entity.Comment;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface CommentRepository extends CrudRepository<Comment, Long> {

    /**
     * Find all comments for a post
     */
    List<Comment> findByPostId(Long postId);

    /**
     * Count comments for a post
     */
    long countByPostId(Long postId);

    /**
     * Delete all comments for a post (useful for post deletion)
     */
    long deleteByPostId(Long postId);
}
