package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "company.find")
@Service
public class FindCompanyService implements FindCompanyUseCase {
    private final CompanyRepository repository;

    public FindCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    public CompanyDto findById(Long id) {
        return repository.findById(id)
            .map(CompanyDto::from)
            .orElseThrow(() -> new CompanyNotFoundException(id));
    }
}
