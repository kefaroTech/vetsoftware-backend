package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.Company;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.create")
@Service
public class CreateCompanyService implements CreateCompanyUseCase {
    private final CompanyRepository repository;

    public CreateCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanyDto execute(CreateCompanyCommand command, AuthContext auth) {
        Company company = Company.create(
            command.name(), command.identifier(), command.address(), command.contactNumber()
        );
        return CompanyDto.from(repository.save(company));
    }
}
