package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindRolePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    RolePermissionDto findById(Long id);
}
