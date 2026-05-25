package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBasePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('base_permission.update') or hasRole('SYSTEM')")
    BasePermissionDto execute(Long id);
}
