package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindLaboratoryTestFileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTestFile.read') or hasRole('SYSTEM')")
    LaboratoryTestFileDto findById(Long id);
}
