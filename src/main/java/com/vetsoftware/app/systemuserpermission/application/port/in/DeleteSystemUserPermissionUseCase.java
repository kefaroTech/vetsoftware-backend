package com.vetsoftware.app.systemuserpermission.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSystemUserPermissionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
