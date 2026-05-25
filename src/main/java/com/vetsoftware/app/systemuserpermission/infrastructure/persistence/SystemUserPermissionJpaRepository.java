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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE system_user_permissions SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsBySystemUser_Id(Long systemUserId);

    boolean existsBySystemPermission_Id(Long systemPermissionId);
}
