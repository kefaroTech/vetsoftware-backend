package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.CreateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.daycare.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.application.port.out.DayCareUsageLimitPort;
import com.vetsoftware.app.daycare.domain.AnimalRef;
import com.vetsoftware.app.daycare.domain.CompanyRef;
import com.vetsoftware.app.daycare.domain.DayCare;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "day.care.create")
@Service
public class CreateDayCareService implements CreateDayCareUseCase {
    private final DayCareRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final DayCareUsageLimitPort usageLimitPort;
    private final Clock clock;

    public CreateDayCareService(DayCareRepository repository, AnimalQueryPort animalQueryPort,
            CompanyQueryPort companyQueryPort, DayCareUsageLimitPort usageLimitPort, Clock clock) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.usageLimitPort = usageLimitPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DayCareDto execute(CreateDayCareCommand command) {
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        usageLimitPort.checkNotExceeded(command.companyId());
        DayCare dayCare = DayCare.create(command.date(), command.startDate(), command.endDate(),
                command.type(), command.objects(), command.observations(), animal, company);
        DayCare saved = repository.save(dayCare);
        usageLimitPort.record(command.companyId(), saved.getId(), LocalDateTime.now(clock));
        return DayCareDto.from(saved);
    }
}
