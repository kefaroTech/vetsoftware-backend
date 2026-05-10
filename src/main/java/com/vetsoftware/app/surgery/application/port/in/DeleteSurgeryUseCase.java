package com.vetsoftware.app.surgery.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSurgeryUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
