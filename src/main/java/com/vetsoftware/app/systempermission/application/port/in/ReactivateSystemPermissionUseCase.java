package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSystemPermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('systempermission.update') or hasRole('SYSTEM')")
    SystemPermissionDto execute(Long id);
}
