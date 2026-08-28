package com.vetsoftware.app.companylimitoverride.application.usecase;

import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ListCompanyLimitOverridesUseCase;
import com.vetsoftware.app.companylimitoverride.application.port.out.CompanyLimitOverrideRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La historia de excepciones de una empresa. Acotado siempre. */
@Service
public class ListCompanyLimitOverridesService implements ListCompanyLimitOverridesUseCase {

    private final CompanyLimitOverrideRepository repository;

    public ListCompanyLimitOverridesService(CompanyLimitOverrideRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyLimitOverrideDto> listByCompanyId(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(CompanyLimitOverrideDto::from)
                .toList();
    }
}
