package com.vetsoftware.app.laboratorytestfile.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeleteLaboratoryTestFileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTestFile.delete') or hasRole('SYSTEM')")
    void execute(Long id);
}
