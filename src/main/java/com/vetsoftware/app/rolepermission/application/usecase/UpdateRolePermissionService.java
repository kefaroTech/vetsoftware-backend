package com.vetsoftware.app.rolepermission.application.usecase;

import com.vetsoftware.app.rolepermission.application.command.UpdateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.in.UpdateRolePermissionUseCase;
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

@Observed(name = "rolepermission.update")
@Service
public class UpdateRolePermissionService implements UpdateRolePermissionUseCase {
    private final RolePermissionRepository repository;
    private final RoleQueryPort roleQueryPort;
    private final PermissionQueryPort permissionQueryPort;

    public UpdateRolePermissionService(RolePermissionRepository repository,
                                       RoleQueryPort roleQueryPort,
                                       PermissionQueryPort permissionQueryPort) {
        this.repository = repository;
        this.roleQueryPort = roleQueryPort;
        this.permissionQueryPort = permissionQueryPort;
    }

    @Override
    @Transactional
    public RolePermissionDto execute(UpdateRolePermissionCommand command) {
        RolePermission rolePermission = repository.findById(command.id())
            .orElseThrow(() -> new RolePermissionNotFoundException(command.id()));
        RoleRef role = roleQueryPort.findById(command.roleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
        PermissionRef permission = permissionQueryPort.findById(command.permissionId())
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + command.permissionId()));
        rolePermission.update(role, permission);
        return RolePermissionDto.from(repository.save(rolePermission));
    }
}
