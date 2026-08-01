package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateBasePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('base_permission.update')")
    BasePermissionDto execute(Long id);
}
