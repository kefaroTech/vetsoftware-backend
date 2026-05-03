package com.vetsoftware.app.role.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
