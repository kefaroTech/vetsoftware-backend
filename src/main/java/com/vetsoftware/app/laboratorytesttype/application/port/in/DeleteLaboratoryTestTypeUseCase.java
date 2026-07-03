package com.vetsoftware.app.laboratorytesttype.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
