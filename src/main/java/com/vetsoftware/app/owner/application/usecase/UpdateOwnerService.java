package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.UpdateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.out.CityQueryPort;
import com.vetsoftware.app.owner.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.CityRef;
import com.vetsoftware.app.owner.domain.CompanyRef;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "owner.update")
@Service
public class UpdateOwnerService implements UpdateOwnerUseCase {
    private final OwnerRepository repository;
    private final CityQueryPort cityQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public UpdateOwnerService(OwnerRepository repository,
                              CityQueryPort cityQueryPort,
                              CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public OwnerDto execute(UpdateOwnerCommand command) {
        Owner owner = repository.findById(command.id())
            .orElseThrow(() -> new OwnerNotFoundException(command.id()));
        CityRef city = cityQueryPort.findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        owner.update(command.name(), command.email(), command.document(), command.documentType(),
                     command.personType(), command.verificationDigit(), command.legalName(),
                     command.address(), command.phone(), city, company, command.withholdingAgent());
        return OwnerDto.from(repository.save(owner));
    }
}
