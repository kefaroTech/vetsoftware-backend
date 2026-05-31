package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.command.CreateLaboratoryTestFileCommand;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateLaboratoryTestFileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.create') or hasRole('SYSTEM')")
    LaboratoryTestFileDto execute(CreateLaboratoryTestFileCommand command);
}
