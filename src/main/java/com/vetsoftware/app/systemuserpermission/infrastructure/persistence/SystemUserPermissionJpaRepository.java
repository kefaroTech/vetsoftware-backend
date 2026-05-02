package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemUserPermissionJpaRepository
        extends JpaRepository<SystemUserPermissionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"systemUser", "systemPermission"})
    List<SystemUserPermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"systemUser", "systemPermission"})
    Optional<SystemUserPermissionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "systemPermission")
    List<SystemUserPermissionJpaEntity> findBySystemUserId(Long systemUserId);
}
