package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.SuspendPendingMedicationSchedulesUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication.schedule.suspend.pending")
@Service
public class SuspendPendingMedicationSchedulesService
    implements SuspendPendingMedicationSchedulesUseCase {
  private final MedicationScheduleRepository repository;

  public SuspendPendingMedicationSchedulesService(MedicationScheduleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public List<MedicationScheduleDto> execute(Long hospitalizationMedicationId) {
    repository.disablePendingByHospitalizationMedicationId(hospitalizationMedicationId);
    // Quedan solo las aplicadas (enabled=true).
    return repository.findByHospitalizationMedicationId(hospitalizationMedicationId).stream()
        .map(MedicationScheduleDto::from)
        .toList();
  }
}
