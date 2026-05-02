package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CityQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.create")
@Service
public class CreateCompanyService implements CreateCompanyUseCase {
    private final CompanyRepository repository;
    private final CityQueryPort cityQueryPort;

    public CreateCompanyService(CompanyRepository repository, CityQueryPort cityQueryPort) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
    }

    @Override
    public CompanyDto execute(CreateCompanyCommand command, AuthContext auth) {
        CityRef city = cityQueryPort.findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
        Company company = Company.create(
            command.name(), command.identifier(), command.address(), command.contactNumber(), city
        );
        return CompanyDto.from(repository.save(company));
    }
}
