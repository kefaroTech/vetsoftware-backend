package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.command.CreateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.CreateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprocedure.domain.DurationMeasure;
import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationprocedure.domain.Frequency;
import com.vetsoftware.app.hospitalizationprocedure.domain.GuidelineType;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.procedure.create")
@Service
public class CreateHospitalizationProcedureService
        implements
            CreateHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;
    private final HospitalizationQueryPort hospitalizationQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public CreateHospitalizationProcedureService(HospitalizationProcedureRepository repository,
            HospitalizationQueryPort hospitalizationQueryPort,
            EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.hospitalizationQueryPort = hospitalizationQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    public HospitalizationProcedureDto execute(CreateHospitalizationProcedureCommand command) {
        HospitalizationRef hospitalization = hospitalizationQueryPort
                .findById(command.hospitalizationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hospitalization not found: " + command.hospitalizationId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById()).orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        HospitalizationProcedure procedure = HospitalizationProcedure.create(command.name(),
                command.dose(), parseFrequency(command.frequency()),
                parseGuidelineType(command.guidelineType()),
                parseDurationMeasure(command.durationMeasure()), command.durationQuantity(),
                command.startDate(), command.startTime(), command.notes(), hospitalization,
                createdBy);
        return HospitalizationProcedureDto.from(repository.save(procedure));
    }

    private Frequency parseFrequency(String s) {
        return s == null || s.isBlank() ? null : Frequency.valueOf(s.trim().toUpperCase());
    }

    private GuidelineType parseGuidelineType(String s) {
        return s == null || s.isBlank() ? null : GuidelineType.valueOf(s.trim().toUpperCase());
    }

    private DurationMeasure parseDurationMeasure(String s) {
        return s == null || s.isBlank() ? null : DurationMeasure.valueOf(s.trim().toUpperCase());
    }
}
