package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivateSystemPermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('systempermission.update')")
    SystemPermissionDto execute(Long id);
}
