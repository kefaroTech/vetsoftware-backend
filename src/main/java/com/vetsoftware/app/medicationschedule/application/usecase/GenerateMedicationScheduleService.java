package com.vetsoftware.app.medicationschedule.application.usecase;

import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.GenerateMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.HospitalizationMedicationQueryPort;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.EmployeeRef;
import com.vetsoftware.app.medicationschedule.domain.MedicationOrderParams;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import com.vetsoftware.app.medicationschedule.domain.MedicationScheduleGenerator;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medication_schedule.generate")
@Service
public class GenerateMedicationScheduleService implements GenerateMedicationScheduleUseCase {
    private final MedicationScheduleRepository repository;
    private final HospitalizationMedicationQueryPort medicationQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public GenerateMedicationScheduleService(MedicationScheduleRepository repository,
                                             HospitalizationMedicationQueryPort medicationQueryPort,
                                             EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.medicationQueryPort = medicationQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public List<MedicationScheduleDto> execute(GenerateMedicationScheduleCommand command) {
        MedicationOrderParams params = medicationQueryPort.findById(command.hospitalizationMedicationId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Hospitalization medication not found: " + command.hospitalizationMedicationId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException(
                "Employee not found: " + command.createdById()));

        // Idempotente: limpia el plan previo antes de regenerar (cubre creación y edición).
        repository.disableByHospitalizationMedicationId(params.id());

        List<MedicationSchedule> generated = MedicationScheduleGenerator.generate(params, createdBy);
        return generated.stream()
            .map(repository::save)
            .map(MedicationScheduleDto::from)
            .toList();
    }
}
