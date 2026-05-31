package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GenerateMedicationScheduleUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.create') or hasRole('SYSTEM')")
    List<MedicationScheduleDto> execute(GenerateMedicationScheduleCommand command);
}
