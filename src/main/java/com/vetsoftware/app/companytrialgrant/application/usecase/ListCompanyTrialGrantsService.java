package com.vetsoftware.app.companytrialgrant.application.usecase;

import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListCompanyTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Qué ha probado ya esta empresa. Acotado siempre. */
@Service
public class ListCompanyTrialGrantsService implements ListCompanyTrialGrantsUseCase {

    private final CompanyTrialGrantRepository repository;

    public ListCompanyTrialGrantsService(CompanyTrialGrantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyTrialGrantDto> listByCompanyId(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(CompanyTrialGrantDto::from)
                .toList();
    }
}
