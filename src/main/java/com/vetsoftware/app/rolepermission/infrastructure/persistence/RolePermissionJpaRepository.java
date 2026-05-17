package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermissionJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"role", "permission"})
    Optional<RolePermissionJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "permission")
    List<RolePermissionJpaEntity> findByRoleIdIn(List<Long> roleIds);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermissionJpaEntity> findAllByRoleId(Long roleId);

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermissionJpaEntity> findAllByRoleCompanyId(Long companyId);
}
