package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.command.UpdateSpaCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.UpdateSpaUseCase;
import com.vetsoftware.app.spa.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.spa.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.application.port.out.SpaTypeQueryPort;
import com.vetsoftware.app.spa.domain.AnimalRef;
import com.vetsoftware.app.spa.domain.CompanyRef;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.spa.domain.SpaTypeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.update")
@Service
public class UpdateSpaService implements UpdateSpaUseCase {
    private final SpaRepository repository;
    private final SpaTypeQueryPort spaTypeQueryPort;
    private final AnimalQueryPort animalQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateSpaService(SpaRepository repository, SpaTypeQueryPort spaTypeQueryPort,
            AnimalQueryPort animalQueryPort, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.spaTypeQueryPort = spaTypeQueryPort;
        this.animalQueryPort = animalQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public SpaDto execute(UpdateSpaCommand command) {
        Spa spa = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new SpaNotFoundException(command.id()));
        Long companyId = command.companyId() == null ? spa.getCompany().id() : command.companyId();
        SpaTypeRef spaType = spaTypeQueryPort.findById(command.spaTypeId()).orElseThrow(
                () -> new IllegalArgumentException("SpaType not found: " + command.spaTypeId()));
        AnimalRef animal = animalQueryPort.findByIdAndCompanyId(command.animalId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal not found: " + command.animalId()));
        CompanyRef company = companyQueryPort.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        spa.update(command.date(), spaType, command.reason(), command.details(),
                command.observations(), animal, company);
        return SpaDto.from(repository.save(spa));
    }
}
