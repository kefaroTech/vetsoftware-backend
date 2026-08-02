package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ApplyMedicationScheduleUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
  List<MedicationScheduleDto> execute(ApplyMedicationScheduleCommand command);
}
