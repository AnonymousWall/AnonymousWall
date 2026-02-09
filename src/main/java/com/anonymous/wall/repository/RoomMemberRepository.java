package com.anonymous.wall.repository;

import com.anonymous.wall.entity.RoomMember;
import com.anonymous.wall.entity.RoomMemberId;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface RoomMemberRepository extends CrudRepository<RoomMember, RoomMemberId> {

    /**
     * Find all rooms a user is a member of
     */
    @Query("SELECT * FROM room_members WHERE user_id = :userId")
    List<RoomMember> findByUserId(UUID userId);

    /**
     * Find all members in a room
     */
    @Query("SELECT * FROM room_members WHERE room_id = :roomId")
    List<RoomMember> findByRoomId(UUID roomId);

    /**
     * Check if a user is a member of a room
     */
    boolean existsById(RoomMemberId id);

    /**
     * Count members in a room
     */
    @Query("SELECT COUNT(*) FROM room_members WHERE room_id = :roomId")
    long countByRoomId(UUID roomId);
}
