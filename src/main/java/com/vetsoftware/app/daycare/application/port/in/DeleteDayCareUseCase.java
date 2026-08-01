package com.vetsoftware.app.daycare.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteDayCareUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('dayCare.delete')")
    void execute(Long id);
}
