package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.CreateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationobservation.domain.EmployeeRef;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization_observation.create")
@Service
public class CreateHospitalizationObservationService implements CreateHospitalizationObservationUseCase {
    private final HospitalizationObservationRepository repository;
    private final HospitalizationQueryPort hospitalizationQueryPort;
    private final EmployeeQueryPort employeeQueryPort;

    public CreateHospitalizationObservationService(HospitalizationObservationRepository repository,
                                                   HospitalizationQueryPort hospitalizationQueryPort,
                                                   EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.hospitalizationQueryPort = hospitalizationQueryPort;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    public HospitalizationObservationDto execute(CreateHospitalizationObservationCommand command) {
        HospitalizationRef hospitalization = hospitalizationQueryPort.findById(command.hospitalizationId())
            .orElseThrow(() -> new IllegalArgumentException("Hospitalization not found: " + command.hospitalizationId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        HospitalizationObservation observation = HospitalizationObservation.create(
            command.description(), hospitalization, createdBy);
        return HospitalizationObservationDto.from(repository.save(observation));
    }
}
