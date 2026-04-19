package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import org.springframework.stereotype.Service;

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
