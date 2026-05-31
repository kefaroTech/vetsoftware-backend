package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ApplyMedicationScheduleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.update') or hasRole('SYSTEM')")
    List<MedicationScheduleDto> execute(ApplyMedicationScheduleCommand command);
}
