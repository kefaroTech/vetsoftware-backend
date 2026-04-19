package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.command.CreateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.Company;
import org.springframework.stereotype.Service;

@Service
public class CreateCompanyService implements CreateCompanyUseCase {
    private final CompanyRepository repository;

    public CreateCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanyDto execute(CreateCompanyCommand command) {
        Company company = Company.create(
            command.name(), command.identifier(), command.address(),
            command.contactNumber(), command.createdBy()
        );
        return CompanyDto.from(repository.save(company));
    }
}
