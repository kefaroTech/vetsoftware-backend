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

    @EntityGraph(attributePaths = {"role", "permission"})
    Optional<RolePermissionJpaEntity> findByIdAndRole_Company_Id(Long id, Long companyId);

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

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE role_permissions rp JOIN roles r ON r.id = rp.role_id SET rp.enabled = true WHERE rp.id = :id AND r.company_id = :companyId", nativeQuery = true)
    int reactivate(@org.springframework.data.repository.query.Param("id") Long id,
                   @org.springframework.data.repository.query.Param("companyId") Long companyId);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE role_permissions SET enabled = false WHERE role_id = :roleId AND enabled = true", nativeQuery = true)
    int disableAllByRoleId(@org.springframework.data.repository.query.Param("roleId") Long roleId);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT id FROM role_permissions WHERE role_id = :roleId AND permission_id = :permissionId AND enabled = false LIMIT 1",
        nativeQuery = true)
    java.util.Optional<Long> findDisabledIdByRoleAndPermission(
        @org.springframework.data.repository.query.Param("roleId") Long roleId,
        @org.springframework.data.repository.query.Param("permissionId") Long permissionId);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT id AS id, permission_id AS permissionId FROM role_permissions WHERE role_id = :roleId AND permission_id IN (:permissionIds) AND enabled = false",
        nativeQuery = true)
    List<DisabledRolePermissionRow> findDisabledByRoleAndPermissions(
        @org.springframework.data.repository.query.Param("roleId") Long roleId,
        @org.springframework.data.repository.query.Param("permissionIds") java.util.Collection<Long> permissionIds);

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(
        value = "UPDATE role_permissions SET enabled = true WHERE id IN (:ids)",
        nativeQuery = true)
    int reactivateAllByIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    boolean existsByRole_Id(Long roleId);

    boolean existsByPermission_Id(Long permissionId);
}
