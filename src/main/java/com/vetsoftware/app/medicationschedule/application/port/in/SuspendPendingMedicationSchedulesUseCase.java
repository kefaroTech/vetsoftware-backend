package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SuspendPendingMedicationSchedulesUseCase {
    /** Soft-delete de las tomas pendientes de una medicación; conserva las aplicadas. */
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.update') or hasRole('SYSTEM')")
    List<MedicationScheduleDto> execute(Long hospitalizationMedicationId);
}
