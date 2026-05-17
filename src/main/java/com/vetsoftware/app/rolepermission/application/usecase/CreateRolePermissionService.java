package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.CreateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionQueryPort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RoleQueryPort;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "rolepermission.create")
@Service
public class CreateRolePermissionService implements CreateRolePermissionUseCase {
    private final RolePermissionRepository repository;
    private final RoleQueryPort roleQueryPort;
    private final PermissionQueryPort permissionQueryPort;
    private final PermissionCachePort permissionCachePort;

    public CreateRolePermissionService(RolePermissionRepository repository,
                                       RoleQueryPort roleQueryPort,
                                       PermissionQueryPort permissionQueryPort,
                                       PermissionCachePort permissionCachePort) {
        this.repository = repository;
        this.roleQueryPort = roleQueryPort;
        this.permissionQueryPort = permissionQueryPort;
        this.permissionCachePort = permissionCachePort;
    }

    @Override
    public RolePermissionDto execute(CreateRolePermissionCommand command) {
        RoleRef role = roleQueryPort.findById(command.roleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
        PermissionRef permission = permissionQueryPort.findById(command.permissionId())
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + command.permissionId()));
        RolePermission rolePermission = RolePermission.create(role, permission);
        RolePermissionDto dto = RolePermissionDto.from(repository.save(rolePermission));
        permissionCachePort.evictByRoleId(command.roleId());
        return dto;
    }
}
