package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseRolePermissionJpaRepository extends JpaRepository<BaseRolePermissionJpaEntity, Long> {
    boolean existsByBaseRoleIdAndBasePermissionId(Long baseRoleId, Long basePermissionId);

    @Override
    @EntityGraph(attributePaths = {"baseRole", "basePermission"})
    List<BaseRolePermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"baseRole", "basePermission"})
    Optional<BaseRolePermissionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = {"basePermission", "basePermission.subModule"})
    List<BaseRolePermissionJpaEntity> findByBaseRoleId(Long baseRoleId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE base_role_permissions SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT id FROM base_role_permissions WHERE base_role_id = :baseRoleId AND base_permission_id = :basePermissionId AND enabled = false LIMIT 1",
        nativeQuery = true)
    Optional<Long> findDisabledIdByBaseRoleAndBasePermission(
        @org.springframework.data.repository.query.Param("baseRoleId") Long baseRoleId,
        @org.springframework.data.repository.query.Param("basePermissionId") Long basePermissionId);

    boolean existsByBaseRole_Id(Long baseRoleId);

    boolean existsByBasePermission_Id(Long basePermissionId);
}
