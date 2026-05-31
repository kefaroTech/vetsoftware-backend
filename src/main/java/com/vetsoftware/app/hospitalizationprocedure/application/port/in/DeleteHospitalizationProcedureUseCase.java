package com.vetsoftware.app.hospitalizationprocedure.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteHospitalizationProcedureUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalizationProcedure.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
