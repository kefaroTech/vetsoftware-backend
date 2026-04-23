package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyRepository;
import com.vetsoftware.app.company.domain.CompanyNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.delete")
@Service
public class DeleteCompanyService implements DeleteCompanyUseCase {
    private final CompanyRepository repository;

    public DeleteCompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
        repository.delete(id);
    }
}
