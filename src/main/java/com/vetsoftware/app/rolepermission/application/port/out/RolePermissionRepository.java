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

    /** Sin acotar: solo el camino SYSTEM ({@code companyId == null}). */
    void deleteAllByIds(List<Long> ids);

    /** Acotada a la empresa: los ids de otro tenant se ignoran. */
    void deleteAllByIds(List<Long> ids, Long companyId);

    int reactivate(Long id);

    int reactivate(Long id, Long companyId);

    /** Sin acotar: solo el camino SYSTEM ({@code companyId == null}). */
    int reactivateAllByIds(Collection<Long> ids);

    /**
     * Acotada a la empresa. Devuelve las filas afectadas, que es como el servicio
     * sabe cuantas revivieron de verdad: un id de otro tenant colado en el lote
     * suma cero.
     */
    int reactivateAllByIds(Collection<Long> ids, Long companyId);

    Optional<Long> findDisabledIdByRoleAndPermission(Long roleId, Long permissionId);

    List<DisabledRolePermissionLookup> findDisabledByRoleAndPermissions(Long roleId,
            Collection<Long> permissionIds);

    record DisabledRolePermissionLookup(Long id, Long permissionId) {
    }
}
