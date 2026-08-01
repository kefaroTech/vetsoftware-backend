package com.vetsoftware.app.laboratorytesttype.application.port.in;

import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateLaboratoryTestTypeUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.update')")
    LaboratoryTestTypeDto execute(UpdateLaboratoryTestTypeCommand command);
}
