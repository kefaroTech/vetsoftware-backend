package com.vetsoftware.app.baserole.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteBaseRoleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
