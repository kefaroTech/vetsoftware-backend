package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.command.UpdateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.UpdateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.update")
@Service
public class UpdateHospitalizationService implements UpdateHospitalizationUseCase {
    private final HospitalizationRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateHospitalizationService(HospitalizationRepository repository,
            AnimalQueryPort animalQueryPort, ConsultationQueryPort consultationQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public HospitalizationDto execute(UpdateHospitalizationCommand command) {
        Hospitalization hospitalization = repository.findById(command.id())
                .orElseThrow(() -> new HospitalizationNotFoundException(command.id()));
        AnimalRef animal = animalQueryPort.findById(command.animalId()).orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort.findById(command.consultationId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        hospitalization.update(command.date(), command.startDate(), command.endDate(),
                command.type(), command.reasonLeaving(), command.reason(), command.observations(),
                animal, consultation, company);
        return HospitalizationDto.from(repository.save(hospitalization));
    }
}
