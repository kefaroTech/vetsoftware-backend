package com.vetsoftware.app.spa.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSpaUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
