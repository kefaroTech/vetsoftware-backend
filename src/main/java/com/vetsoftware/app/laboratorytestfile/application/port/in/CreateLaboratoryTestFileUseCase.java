package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.command.CreateLaboratoryTestFileCommand;
import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateLaboratoryTestFileUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.create') and @authz.isMyCompany(#command.companyId))")
    LaboratoryTestFileDto execute(CreateLaboratoryTestFileCommand command);
}
