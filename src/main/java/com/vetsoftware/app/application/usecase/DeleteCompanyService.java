package com.vetsoftware.app.application.usecase;

import com.vetsoftware.app.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.application.port.out.CompanyRepository;
import com.vetsoftware.app.domain.CompanyNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCompanyService implements DeleteCompanyUseCase {
    private final CompanyRepository repository;

    public DeleteCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
        repository.delete(id);
    }
}
