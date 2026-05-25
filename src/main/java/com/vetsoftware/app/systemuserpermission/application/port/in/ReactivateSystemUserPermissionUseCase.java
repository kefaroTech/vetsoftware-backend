package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSystemUserPermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('systemuserpermission.update') or hasRole('SYSTEM')")
    SystemUserPermissionDto execute(Long id);
}
