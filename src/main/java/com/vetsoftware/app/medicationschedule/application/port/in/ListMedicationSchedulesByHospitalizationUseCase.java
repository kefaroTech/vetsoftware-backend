package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMedicationSchedulesByHospitalizationUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('hospitalization.read')")
  List<MedicationScheduleDto> listByHospitalization(Long hospitalizationId);
}
