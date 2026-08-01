package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListRolePermissionsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('rolePermissions.read')")
    List<RolePermissionDto> listAll();
}
