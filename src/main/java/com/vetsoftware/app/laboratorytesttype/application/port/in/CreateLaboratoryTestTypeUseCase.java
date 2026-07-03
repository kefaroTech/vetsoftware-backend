package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.command.CreateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateLaboratoryTestTypeUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.create') or hasRole('SYSTEM')")
    LaboratoryTestTypeDto execute(CreateLaboratoryTestTypeCommand command);
}
