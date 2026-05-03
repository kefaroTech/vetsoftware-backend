package com.vetsoftware.app.baserolepermission.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteBaseRolePermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
