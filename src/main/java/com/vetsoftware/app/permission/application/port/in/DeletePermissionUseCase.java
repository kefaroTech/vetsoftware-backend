package com.vetsoftware.app.permission.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
