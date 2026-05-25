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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE role_permissions SET enabled = true WHERE id = :id", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByRole_Id(Long roleId);

    boolean existsByPermission_Id(Long permissionId);
}
