package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SyncRolePermissionsUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('rolePermissions.create')")
  List<RolePermissionDto> execute(SyncRolePermissionsCommand command);
}
