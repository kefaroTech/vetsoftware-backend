package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateRolePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    RolePermissionDto execute(CreateRolePermissionCommand command);
}
