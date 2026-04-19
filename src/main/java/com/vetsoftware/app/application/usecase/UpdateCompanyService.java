package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import com.vetsoftware.app.domain.CompanyId;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCompanyService implements UpdateCompanyUseCase {
    private final CompanyRepository repository;

    public UpdateCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanyDto execute(UpdateCompanyCommand command) {
        Company company = repository.findById(CompanyId.of(command.id()))
            .orElseThrow(() -> new CompanyNotFoundException(command.id()));
        company.update(command.name(), command.identifier(), command.address(), command.contactNumber());
        repository.save(company);
        return CompanyDto.from(company);
    }
}
