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
}
