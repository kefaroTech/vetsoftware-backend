package com.vetsoftware.app.vaccination.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteVaccinationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
