package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeLaboratoryTestStatusUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.update')")
  LaboratoryTestDto execute(ChangeLaboratoryTestStatusCommand command);
}
