package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.command.CreateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateLaboratoryTestTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.create') and @authz.isMyCompany(#command.companyId))")
    LaboratoryTestTypeDto execute(CreateLaboratoryTestTypeCommand command);
}
