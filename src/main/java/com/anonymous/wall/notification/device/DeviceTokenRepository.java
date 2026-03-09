package com.anonymous.wall.notification.device;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface DeviceTokenRepository extends CrudRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByDeviceToken(String deviceToken);

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);
}
