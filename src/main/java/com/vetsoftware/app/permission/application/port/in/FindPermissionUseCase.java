package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    PermissionDto findById(Long id);
}
