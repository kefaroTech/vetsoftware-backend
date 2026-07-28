package com.vetsoftware.app.animalalert.application.usecase;

import com.vetsoftware.app.animalalert.application.command.CreateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.in.CreateAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.out.AnimalAlertRepository;
import com.vetsoftware.app.animalalert.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.animalalert.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.domain.AnimalRef;
import com.vetsoftware.app.animalalert.domain.CompanyRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "animal.alert.create")
@Service
public class CreateAnimalAlertService implements CreateAnimalAlertUseCase {
    private final AnimalAlertRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateAnimalAlertService(AnimalAlertRepository repository,
                                    AnimalQueryPort animalQueryPort,
                                    CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public AnimalAlertDto execute(CreateAnimalAlertCommand command) {
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));

        AnimalAlert alert = AnimalAlert.create(
            animal, command.type(), command.description(), command.severity(), company);
        return AnimalAlertDto.from(repository.save(alert));
    }
}
