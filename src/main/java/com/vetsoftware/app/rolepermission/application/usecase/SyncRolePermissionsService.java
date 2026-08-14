package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.SyncRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionQueryPort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository.DisabledRolePermissionLookup;
import com.vetsoftware.app.rolepermission.application.port.out.RoleQueryPort;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.permission.sync")
@Service
public class SyncRolePermissionsService implements SyncRolePermissionsUseCase {
    private final RolePermissionRepository repository;
    private final RoleQueryPort roleQueryPort;
    private final PermissionQueryPort permissionQueryPort;
    private final PermissionCachePort permissionCachePort;

    public SyncRolePermissionsService(RolePermissionRepository repository,
            RoleQueryPort roleQueryPort, PermissionQueryPort permissionQueryPort,
            PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.roleQueryPort = roleQueryPort;
        this.permissionQueryPort = permissionQueryPort;
        this.permissionCachePort = permissionCachePort;
    }

    @Override
    @Transactional
    public List<RolePermissionDto> execute(SyncRolePermissionsCommand command) {
        if (command.roleId() == null) {
            throw new IllegalArgumentException("roleId is required");
        }
        // El companyId null es el principal SYSTEM, cross-tenant por diseno; un
        // empleado
        // solo alcanza los roles de su empresa (BE-29).
        RoleRef role = (command.companyId() == null
                ? roleQueryPort.findById(command.roleId())
                : roleQueryPort.findByIdAndCompanyId(command.roleId(), command.companyId()))
                .orElseThrow(
                        () -> new IllegalArgumentException("Role not found: " + command.roleId()));

        Set<Long> desired = command.permissionIds() == null
                ? new HashSet<>()
                : new HashSet<>(command.permissionIds());

        List<RolePermission> existing = repository.findAllByRoleId(command.roleId());
        Map<Long, Long> existingPermToRpId = new HashMap<>();
        for (RolePermission rp : existing) {
            existingPermToRpId.put(rp.getPermission().id(), rp.getId());
        }

        List<Long> toRemoveRpIds = existingPermToRpId.entrySet().stream()
                .filter(e -> !desired.contains(e.getKey())).map(Map.Entry::getValue).toList();

        Set<Long> toAddPermIds = new HashSet<>(desired);
        toAddPermIds.removeAll(existingPermToRpId.keySet());

        if (!toRemoveRpIds.isEmpty()) {
            repository.deleteAllByIds(toRemoveRpIds);
        }

        if (!toAddPermIds.isEmpty()) {
            // Reactivar las filas desactivadas que coincidan con la clave única
            List<DisabledRolePermissionLookup> disabled = repository
                    .findDisabledByRoleAndPermissions(command.roleId(), toAddPermIds);
            Set<Long> reactivatedPermIds = new HashSet<>();
            if (!disabled.isEmpty()) {
                List<Long> idsToReactivate = disabled.stream().map(DisabledRolePermissionLookup::id)
                        .toList();
                repository.reactivateAllByIds(idsToReactivate);
                disabled.forEach(d -> reactivatedPermIds.add(d.permissionId()));
            }

            // Crear los que no existían (ni activos, ni desactivados)
            Set<Long> toCreatePermIds = new HashSet<>(toAddPermIds);
            toCreatePermIds.removeAll(reactivatedPermIds);
            if (!toCreatePermIds.isEmpty()) {
                List<RolePermission> nuevos = toCreatePermIds.stream().map(pid -> {
                    PermissionRef permission = permissionQueryPort.findById(pid).orElseThrow(
                            () -> new IllegalArgumentException("Permission not found: " + pid));
                    return RolePermission.create(role, permission);
                }).toList();
                repository.saveAll(nuevos);
            }
        }

        List<RolePermissionDto> result = repository.findAllByRoleId(command.roleId()).stream()
                .map(RolePermissionDto::from).toList();
        permissionCachePort.evictByRoleId(command.roleId());
        return result;
    }
}
