package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateLaboratoryTestUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.create')")
    LaboratoryTestDto execute(CreateLaboratoryTestCommand command);
}
