package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GenerateMedicationScheduleUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.create')")
  List<MedicationScheduleDto> execute(GenerateMedicationScheduleCommand command);
}
