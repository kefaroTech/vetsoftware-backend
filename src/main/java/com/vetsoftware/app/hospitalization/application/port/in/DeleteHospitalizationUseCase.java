package com.vetsoftware.app.hospitalization.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    void execute(Long id);
}
