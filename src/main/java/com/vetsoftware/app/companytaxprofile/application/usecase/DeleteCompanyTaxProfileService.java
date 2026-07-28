package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.port.in.DeleteCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.tax.profile.delete")
@Service
public class DeleteCompanyTaxProfileService implements DeleteCompanyTaxProfileUseCase {
    private final CompanyTaxProfileRepository repository;

    public DeleteCompanyTaxProfileService(CompanyTaxProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long companyId) {
        if (!repository.existsByCompanyId(companyId)) {
            throw new CompanyTaxProfileNotFoundException(companyId);
        }
        repository.delete(companyId);
    }
}
