package com.vetsoftware.app.companytrialwindow.application.usecase;

import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.FindCurrentTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.out.CompanyTrialWindowRepository;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindowNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** La ventana viva de una empresa. */
@Service
public class FindCurrentTrialWindowService implements FindCurrentTrialWindowUseCase {

    private final CompanyTrialWindowRepository repository;

    public FindCurrentTrialWindowService(CompanyTrialWindowRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyTrialWindowDto findOpenByCompanyId(Long companyId) {
        return CompanyTrialWindowDto.from(repository.findOpenByCompanyId(companyId)
                .orElseThrow(() -> new CompanyTrialWindowNotFoundException(companyId)));
    }
}
