package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CityQueryPort;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CityRef;
import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.update")
@Service
public class UpdateCompanyService implements UpdateCompanyUseCase {
    private final CompanyRepository repository;
    private final CityQueryPort cityQueryPort;

    public UpdateCompanyService(CompanyRepository repository, CityQueryPort cityQueryPort) {
        this.repository = repository;
        this.cityQueryPort = cityQueryPort;
    }

    @Override
    @Transactional
    public CompanyDto execute(UpdateCompanyCommand command, AuthContext auth) {
        Company company = repository.findById(command.id())
            .orElseThrow(() -> new CompanyNotFoundException(command.id()));
        CityRef city = cityQueryPort.findById(command.cityId())
            .orElseThrow(() -> new IllegalArgumentException("City not found: " + command.cityId()));
        company.update(command.name(), command.identifier(), command.address(),
            command.contactNumber(), city);
        return CompanyDto.from(repository.save(company));
    }
}
