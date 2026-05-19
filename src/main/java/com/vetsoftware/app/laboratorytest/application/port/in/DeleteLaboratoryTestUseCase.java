package com.vetsoftware.app.laboratorytest.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
