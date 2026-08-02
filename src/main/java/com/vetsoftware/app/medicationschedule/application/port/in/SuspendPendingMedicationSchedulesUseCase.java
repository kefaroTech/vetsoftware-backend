package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SuspendPendingMedicationSchedulesUseCase {
    /**
     * Soft-delete de las tomas pendientes de una medicación; conserva las
     * aplicadas.
     */
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.update')")
    List<MedicationScheduleDto> execute(Long hospitalizationMedicationId);
}
