package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.command.UpdateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.UpdateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionQueryPort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RoleQueryPort;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.permission.update")
@Service
public class UpdateRolePermissionService implements UpdateRolePermissionUseCase {
    private final RolePermissionRepository repository;
    private final RoleQueryPort roleQueryPort;
    private final PermissionQueryPort permissionQueryPort;
    private final PermissionCachePort permissionCachePort;

    public UpdateRolePermissionService(RolePermissionRepository repository,
                                       RoleQueryPort roleQueryPort,
                                       PermissionQueryPort permissionQueryPort,
                                       PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.roleQueryPort = roleQueryPort;
        this.permissionQueryPort = permissionQueryPort;
        this.permissionCachePort = permissionCachePort;
    }

    @Override
    @Transactional
    public RolePermissionDto execute(UpdateRolePermissionCommand command) {
        RolePermission rolePermission = (command.companyId() == null
            ? repository.findById(command.id())
            : repository.findByIdAndCompanyId(command.id(), command.companyId()))
            .orElseThrow(() -> new RolePermissionNotFoundException(command.id()));
        Long previousRoleId = rolePermission.getRole().id();
        RoleRef role = (command.companyId() == null
            ? roleQueryPort.findById(command.roleId())
            : roleQueryPort.findByIdAndCompanyId(command.roleId(), command.companyId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
        PermissionRef permission = (command.companyId() == null
            ? permissionQueryPort.findById(command.permissionId())
            : permissionQueryPort.findByIdAndCompanyId(command.permissionId(), command.companyId()))
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + command.permissionId()));
        rolePermission.update(role, permission);
        RolePermissionDto dto = RolePermissionDto.from(repository.save(rolePermission));
        permissionCachePort.evictByRoleId(previousRoleId);
        if (!previousRoleId.equals(command.roleId())) {
            permissionCachePort.evictByRoleId(command.roleId());
        }
        return dto;
    }
}
