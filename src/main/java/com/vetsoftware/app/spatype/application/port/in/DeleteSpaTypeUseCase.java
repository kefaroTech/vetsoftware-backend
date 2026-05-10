package com.vetsoftware.app.spatype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSpaTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
