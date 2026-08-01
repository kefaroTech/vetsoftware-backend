package com.vetsoftware.app.laboratorytestfile.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestFileUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.delete')")
    void execute(Long id);
}
