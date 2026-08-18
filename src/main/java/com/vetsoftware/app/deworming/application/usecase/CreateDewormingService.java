package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.CreateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.deworming.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.deworming.application.port.out.ConsultationQueryPort;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.AnimalRef;
import com.vetsoftware.app.deworming.domain.CompanyRef;
import com.vetsoftware.app.deworming.domain.ConsultationRef;
import com.vetsoftware.app.deworming.domain.Deworming;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "deworming.create")
@Service
public class CreateDewormingService implements CreateDewormingUseCase {
    private final DewormingRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ConsultationQueryPort consultationQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateDewormingService(DewormingRepository repository, AnimalQueryPort animalQueryPort,
            ConsultationQueryPort consultationQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.consultationQueryPort = consultationQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public DewormingDto execute(CreateDewormingCommand command) {
        // Referencias acotadas: el companyId lo inyecta el controller desde el
        // principal (authz.currentCompanyId()), asi que nunca es null aqui.
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        ConsultationRef consultation = command.consultationId() == null
                ? null
                : consultationQueryPort
                        .findByIdAndCompanyId(command.consultationId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Consultation not found: " + command.consultationId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        Deworming deworming = Deworming.create(command.date(), command.lastDeworming(),
                command.type(), command.product(), command.dosage(), command.nextControl(),
                command.observations(), animal, consultation, company);
        return DewormingDto.from(repository.save(deworming));
    }
}
