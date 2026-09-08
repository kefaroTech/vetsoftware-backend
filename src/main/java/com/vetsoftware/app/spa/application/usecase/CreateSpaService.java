package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.command.CreateSpaCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.CreateSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.spa.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaTypeQueryPort;
import com.vetsoftware.app.spa.application.port.out.SpaUsageLimitPort;
import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.create")
@Service
public class CreateSpaService implements CreateSpaUseCase {
    private final SpaRepository repository;
    private final SpaTypeQueryPort spaTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;
    private final SpaUsageLimitPort usageLimitPort;
    private final Clock clock;

    public CreateSpaService(SpaRepository repository, SpaTypeQueryPort spaTypeQueryPort,
            AnimalQueryPort animalQueryPort, CompanyQueryPort companyQueryPort,
            SpaUsageLimitPort usageLimitPort, Clock clock) {
        this.repository = repository;
        this.spaTypeQueryPort = spaTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
        this.usageLimitPort = usageLimitPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SpaDto execute(CreateSpaCommand command) {
        SpaTypeRef spaType = spaTypeQueryPort.findById(command.spaTypeId()).orElseThrow(
                () -> new IllegalArgumentException("SpaType not found: " + command.spaTypeId()));
        // Referencia acotada, igual que UpdateSpaService: sin el companyId, un
        // animalId de otro tenant colgaba esta estancia de spa de la historia clinica
        // de la vecina. El companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId()), asi que nunca es null aqui.
        AnimalRef animal = animalQueryPort
                .findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));

        usageLimitPort.checkNotExceeded(command.companyId());
        Spa spa = Spa.create(command.date(), spaType, command.reason(), command.details(),
                command.observations(), animal, company);
        Spa saved = repository.save(spa);
        usageLimitPort.record(command.companyId(), saved.getId(), LocalDateTime.now(clock));
        return SpaDto.from(saved);
    }
}
