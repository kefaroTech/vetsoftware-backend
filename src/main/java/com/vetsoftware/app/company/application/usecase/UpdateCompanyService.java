package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.update")
@Service
public class UpdateCompanyService implements UpdateCompanyUseCase {
    private final CompanyRepository repository;

    public UpdateCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanyDto execute(UpdateCompanyCommand command) {
        Company company = repository.findById(command.id())
            .orElseThrow(() -> new CompanyNotFoundException(command.id()));
        company.update(command.name(), command.identifier(), command.address(), command.contactNumber());
        return CompanyDto.from(repository.save(company));
    }
}
