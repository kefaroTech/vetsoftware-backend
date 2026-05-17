package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SyncRolePermissionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('rolePermissions.create') or hasRole('SYSTEM')")
    List<RolePermissionDto> execute(SyncRolePermissionsCommand command);
}
