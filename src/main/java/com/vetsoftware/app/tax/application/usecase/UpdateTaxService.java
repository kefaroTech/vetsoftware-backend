package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.command.UpdateTaxCommand;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.UpdateTaxUseCase;
import com.vetsoftware.app.tax.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import com.vetsoftware.app.tax.domain.TaxNameAlreadyExistsException;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "tax.update")
@Service
public class UpdateTaxService implements UpdateTaxUseCase {
    private final TaxRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateTaxService(TaxRepository repository,
                            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public TaxDto execute(UpdateTaxCommand command) {
        Tax tax = repository.findById(command.id())
                .orElseThrow(() -> new TaxNotFoundException(command.id()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndNameExcludingId(command.companyId(), command.name(), command.id())) {
            throw new TaxNameAlreadyExistsException(command.name());
        }
        tax.update(command.name(), command.percentage(), command.taxScheme(), company, command.updatedBy());
        return TaxDto.from(repository.save(tax));
    }
}
