package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.command.CreateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.CreateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.AnimalWeightPort;
import com.vetsoftware.app.hospitalization.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.AnimalRef;
import com.vetsoftware.app.hospitalization.domain.CompanyRef;
import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.create")
@Service
public class CreateHospitalizationService implements CreateHospitalizationUseCase {
    private final HospitalizationRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final AnimalWeightPort animalWeightPort;

    public CreateHospitalizationService(HospitalizationRepository repository,
                                        AnimalQueryPort animalQueryPort,
                                        ConsultationQueryPort consultationQueryPort,
                                        CompanyQueryPort companyQueryPort,
                                        AnimalWeightPort animalWeightPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.animalWeightPort = animalWeightPort;
    }

    @Override
    @Transactional
    public HospitalizationDto execute(CreateHospitalizationCommand command) {
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null ? null
            : consultationQueryPort.findById(command.consultationId())
                .orElseThrow(() -> new IllegalArgumentException("Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));

        Hospitalization hospitalization = Hospitalization.create(
            command.date(), command.startDate(), command.endDate(),
            command.type(), command.reasonLeaving(),
            command.reason(), command.observations(),
            animal, consultation, company);
        Hospitalization saved = repository.save(hospitalization);

        // Peso opcional al ingreso → punto de la serie temporal del animal (misma transacción).
        if (command.weight() != null) {
            animalWeightPort.recordHospitalizationWeight(command.animalId(), command.companyId(),
                command.weight(), command.weightUnit(), command.date(), saved.getId());
        }
        return HospitalizationDto.from(saved);
    }
}
