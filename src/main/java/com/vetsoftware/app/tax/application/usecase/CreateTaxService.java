package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.command.CreateTaxCommand;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.CreateTaxUseCase;
import com.vetsoftware.app.tax.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "tax.create")
@Service
public class CreateTaxService implements CreateTaxUseCase {
    private final TaxRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateTaxService(TaxRepository repository, CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public TaxDto execute(CreateTaxCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndName(command.companyId(), command.name())) {
            throw new TaxNameAlreadyExistsException(command.name());
        }
        return TaxDto.from(repository.save(
                Tax.create(command.name(), command.percentage(), command.taxScheme(), company)));
    }
}
