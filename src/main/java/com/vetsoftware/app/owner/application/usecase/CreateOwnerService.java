package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.CreateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.out.CityQueryPort;
import com.vetsoftware.app.owner.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.CityRef;
import com.vetsoftware.app.owner.domain.CompanyRef;
import com.vetsoftware.app.owner.domain.Owner;
import com.vetsoftware.app.owner.domain.TaxRegime;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "owner.create")
@Service
public class CreateOwnerService implements CreateOwnerUseCase {
    private final OwnerRepository repository;
    private final CityQueryPort cityQueryPort;
    private final CompanyQueryPort companyQueryPort;

    public CreateOwnerService(OwnerRepository repository,
                              CityQueryPort cityQueryPort,
                              CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public OwnerDto execute(CreateOwnerCommand command) {
        CityRef city = cityQueryPort.findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        // Si el request no trae régimen, se infiere (jurídica/NIT → Responsable de IVA).
        TaxRegime taxRegime = command.taxRegime() != null
            ? command.taxRegime()
            : TaxRegime.defaultFor(command.personType(), command.documentType());
        Owner owner = Owner.create(
            command.name(), command.email(), command.document(), command.documentType(),
            command.personType(), command.verificationDigit(), command.legalName(),
            command.address(), command.phone(), city, company, command.withholdingAgent(), taxRegime
        );
        return OwnerDto.from(repository.save(owner));
    }
}
