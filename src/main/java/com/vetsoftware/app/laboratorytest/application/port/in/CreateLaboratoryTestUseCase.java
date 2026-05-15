package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateLaboratoryTestUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.create') or hasRole('SYSTEM')")
    LaboratoryTestDto execute(CreateLaboratoryTestCommand command);
}
