package com.vetsoftware.app.surgerytype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteSurgeryTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
