package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.CreateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationmedication.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationmedication.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationmedication.domain.Frequency;
import com.vetsoftware.app.hospitalizationmedication.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.medication.create")
@Service
public class CreateHospitalizationMedicationService
        implements
            CreateHospitalizationMedicationUseCase {
    private final HospitalizationMedicationRepository repository;
    private final HospitalizationQueryPort hospitalizationQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public CreateHospitalizationMedicationService(HospitalizationMedicationRepository repository,
            HospitalizationQueryPort hospitalizationQueryPort,
            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.hospitalizationQueryPort = hospitalizationQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    public HospitalizationMedicationDto execute(CreateHospitalizationMedicationCommand command) {
        // Referencia acotada: el companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId()), asi que nunca es null aqui. Una hospitalizacion
        // de
        // otro tenant no se resuelve y la creacion falla con el mismo "not found" que
        // un
        // id inexistente: quien lo intenta no llega a saber si la fila existe.
        HospitalizationRef hospitalization = hospitalizationQueryPort
                .findByIdAndCompanyId(command.hospitalizationId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hospitalization not found: " + command.hospitalizationId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById()).orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        HospitalizationMedication medication = HospitalizationMedication.create(command.name(),
                command.dose(), parseFrequency(command.frequency()),
                parseGuidelineType(command.guidelineType()),
                parseDurationMeasure(command.durationMeasure()), command.durationQuantity(),
                command.startDate(), command.startTime(), command.notes(), hospitalization,
                createdBy);
        return HospitalizationMedicationDto.from(repository.save(medication));
    }

    private static Frequency parseFrequency(String s) {
        return s == null || s.isBlank() ? null : Frequency.valueOf(s.trim().toUpperCase());
    }

    private static GuidelineType parseGuidelineType(String s) {
        return s == null || s.isBlank() ? null : GuidelineType.valueOf(s.trim().toUpperCase());
    }

    private static DurationMeasure parseDurationMeasure(String s) {
        return s == null || s.isBlank() ? null : DurationMeasure.valueOf(s.trim().toUpperCase());
    }
}
