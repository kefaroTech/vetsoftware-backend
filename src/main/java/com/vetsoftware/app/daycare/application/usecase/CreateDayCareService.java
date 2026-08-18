package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.CreateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.daycare.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "day.care.create")
@Service
public class CreateDayCareService implements CreateDayCareUseCase {
    private final DayCareRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateDayCareService(DayCareRepository repository, AnimalQueryPort animalQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public DayCareDto execute(CreateDayCareCommand command) {
        // Mismo puerto acotado que el update: nacer apuntando al animal de otro tenant
        // es la misma fuga que reapuntarse a el despues.
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        DayCare dayCare = DayCare.create(command.date(), command.startDate(), command.endDate(),
                command.type(), command.objects(), command.observations(), animal, company);
        return DayCareDto.from(repository.save(dayCare));
    }
}
