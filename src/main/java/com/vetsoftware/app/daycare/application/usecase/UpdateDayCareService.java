package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.command.UpdateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.UpdateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.daycare.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "day.care.update")
@Service
public class UpdateDayCareService implements UpdateDayCareUseCase {
    private final DayCareRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateDayCareService(DayCareRepository repository, AnimalQueryPort animalQueryPort,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public DayCareDto execute(UpdateDayCareCommand command) {
        DayCare dayCare = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new DayCareNotFoundException(command.id()));
        Long companyId = command.companyId() == null
                ? dayCare.getCompany().id()
                : command.companyId();
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        dayCare.update(command.date(), command.startDate(), command.endDate(), command.type(),
                command.objects(), command.observations(), animal, company);
        return DayCareDto.from(repository.save(dayCare));
    }
}
