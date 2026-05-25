package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('permission.update') or hasRole('SYSTEM')")
    PermissionDto execute(Long id);
}
