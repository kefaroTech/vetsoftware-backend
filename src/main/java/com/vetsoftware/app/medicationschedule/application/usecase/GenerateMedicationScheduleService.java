package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.GenerateMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.AppliedStatus;
import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import com.vetsoftware.app.medicationschedule.domain.MedicationScheduleGenerator;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication.schedule.generate")
@Service
public class GenerateMedicationScheduleService implements GenerateMedicationScheduleUseCase {
  private final MedicationScheduleRepository repository;
  private final HospitalizationMedicationQueryPort medicationQueryPort;
  private final EmployeeQueryPort employeeQueryPort;

  public GenerateMedicationScheduleService(
      MedicationScheduleRepository repository,
      HospitalizationMedicationQueryPort medicationQueryPort,
      EmployeeQueryPort employeeQueryPort) {
    this.repository = repository;
    this.medicationQueryPort = medicationQueryPort;
    this.employeeQueryPort = employeeQueryPort;
  }

  @Override
  @Transactional
  public List<MedicationScheduleDto> execute(GenerateMedicationScheduleCommand command) {
    MedicationOrderParams params =
        medicationQueryPort
            .findById(command.hospitalizationMedicationId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Hospitalization medication not found: "
                            + command.hospitalizationMedicationId()));
    EmployeeRef createdBy =
        employeeQueryPort
            .findById(command.createdById())
            .orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.createdById()));

    // Regla de integridad: las tomas APLICADAS son histórico inmutable; solo se
    // recalculan las pendientes.
    List<MedicationSchedule> applied =
        repository.findByHospitalizationMedicationId(params.id()).stream()
            .filter(s -> s.getAppliedStatus() == AppliedStatus.APPLIED)
            .toList();

    List<MedicationSchedule> result = new ArrayList<>();
    if (applied.isEmpty()) {
      // Alta nueva o sin aplicadas: regeneración completa (idempotente).
      repository.disableByHospitalizationMedicationId(params.id());
      for (MedicationSchedule s : MedicationScheduleGenerator.generate(params, createdBy)) {
        result.add(repository.save(s));
      }
    } else {
      // Conserva las aplicadas; reconstruye solo las pendientes.
      repository.disablePendingByHospitalizationMedicationId(params.id());
      LocalDateTime lastApplied =
          applied.stream()
              .map(MedicationSchedule::getRealDateTime)
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder())
              .orElse(null);
      List<MedicationSchedule> pending =
          MedicationScheduleGenerator.generatePending(
              params, applied.size(), lastApplied, createdBy);
      result.addAll(applied);
      for (MedicationSchedule s : pending) {
        result.add(repository.save(s));
      }
    }
    return result.stream().map(MedicationScheduleDto::from).toList();
  }
}
