package com.vetsoftware.app.rolepermission.application.port.out;

import com.vetsoftware.app.rolepermission.domain.RolePermission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository {
    RolePermission save(RolePermission rolePermission);

    List<RolePermission> saveAll(List<RolePermission> rolePermissions);

    Optional<RolePermission> findById(Long id);

    Optional<RolePermission> findByIdAndCompanyId(Long id, Long companyId);

    List<RolePermission> findAll();

    List<RolePermission> findAllByRoleId(Long roleId);

    List<RolePermission> findAllByRoleCompanyId(Long companyId);

    void delete(Long id);

    void deleteAllByIds(List<Long> ids);

    int reactivate(Long id);

    int reactivate(Long id, Long companyId);

    int reactivateAllByIds(Collection<Long> ids);

    Optional<Long> findDisabledIdByRoleAndPermission(Long roleId, Long permissionId);

    List<DisabledRolePermissionLookup> findDisabledByRoleAndPermissions(Long roleId,
            Collection<Long> permissionIds);

    record DisabledRolePermissionLookup(Long id, Long permissionId) {
    }
}
