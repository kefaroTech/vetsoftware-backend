package com.vetsoftware.app.daycare.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDayCareUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('dayCare.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
