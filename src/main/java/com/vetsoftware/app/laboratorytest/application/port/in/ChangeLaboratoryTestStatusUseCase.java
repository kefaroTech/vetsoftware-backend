package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeLaboratoryTestStatusUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    LaboratoryTestDto execute(ChangeLaboratoryTestStatusCommand command);
}
