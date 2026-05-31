package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMedicationSchedulesByHospitalizationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('hospitalization.read') or hasRole('SYSTEM')")
    List<MedicationScheduleDto> listByHospitalization(Long hospitalizationId);
}
