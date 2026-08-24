package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CityQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Observed(name = "company.create")
@Service
public class CreateCompanyService implements CreateCompanyUseCase {
    private final CompanyRepository repository;
    private final CityQueryPort cityQueryPort;
    private final Clock clock;

    public CreateCompanyService(CompanyRepository repository, CityQueryPort cityQueryPort,
            Clock clock) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
        this.clock = clock;
    }

    @Override
    public CompanyDto execute(CreateCompanyCommand command) {
        CityRef city = cityQueryPort.findById(command.cityId()).orElseThrow(
                () -> new IllegalArgumentException("City not found: " + command.cityId()));
        Company company = Company.create(command.name(), command.identifier(), command.address(),
                command.contactNumber(), city, LocalDateTime.now(clock));
        return CompanyDto.from(repository.save(company));
    }
}
