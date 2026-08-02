package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('permission.update')")
    PermissionDto execute(Long id);
}
