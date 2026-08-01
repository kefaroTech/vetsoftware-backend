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
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "role.permission.create")
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
    @Transactional
    public RolePermissionDto execute(CreateRolePermissionCommand command) {
        RoleRef role = (command.companyId() == null
            ? roleQueryPort.findById(command.roleId())
            : roleQueryPort.findByIdAndCompanyId(command.roleId(), command.companyId()))
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + command.roleId()));
        PermissionRef permission = (command.companyId() == null
            ? permissionQueryPort.findById(command.permissionId())
            : permissionQueryPort.findByIdAndCompanyId(command.permissionId(), command.companyId()))
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + command.permissionId()));

        Optional<Long> disabledId = repository
            .findDisabledIdByRoleAndPermission(command.roleId(), command.permissionId());
        if (disabledId.isPresent()) {
            Long id = disabledId.get();
            repository.reactivate(id);
            RolePermission refreshed = repository.findById(id)
                .orElseThrow(() -> new RolePermissionNotFoundException(id));
            RolePermissionDto dto = RolePermissionDto.from(refreshed);
            permissionCachePort.evictByRoleId(command.roleId());
            return dto;
        }

        RolePermission rolePermission = RolePermission.create(role, permission);
        RolePermissionDto dto = RolePermissionDto.from(repository.save(rolePermission));
        permissionCachePort.evictByRoleId(command.roleId());
        return dto;
    }
}
