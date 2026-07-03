package com.vetsoftware.app.vaccinationtype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteVaccinationTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('vaccination.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
