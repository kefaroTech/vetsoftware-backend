package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.ApplyMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication.schedule.apply")
@Service
public class ApplyMedicationScheduleService implements ApplyMedicationScheduleUseCase {
  private final MedicationScheduleRepository repository;

  public ApplyMedicationScheduleService(MedicationScheduleRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public List<MedicationScheduleDto> execute(ApplyMedicationScheduleCommand command) {
    MedicationSchedule target =
        repository
            .findById(command.scheduleId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Medication schedule not found: " + command.scheduleId()));
    target.apply(LocalDateTime.now()); // appliedStatus=APPLIED, realDateTime=now
    repository.save(target);

    // Pauta INTERVALO: aplicar tarde NO recalcula las siguientes; eso solo ocurre al
    // reprogramar una toma (drag&drop → reschedule mode=cascade).
    return repository
        .findByHospitalizationMedicationId(target.getHospitalizationMedication().id())
        .stream()
        .map(MedicationScheduleDto::from)
        .toList();
  }
}
