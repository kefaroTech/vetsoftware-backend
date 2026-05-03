package com.vetsoftware.app.systemuser.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSystemUserUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
