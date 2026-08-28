package com.vetsoftware.app.companytrialwindow.application.usecase;

import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.CloseTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.out.CompanyTrialWindowRepository;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindowNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cierra el reloj de la empresa. */
@Service
public class CloseTrialWindowService implements CloseTrialWindowUseCase {

    private final CompanyTrialWindowRepository repository;
    private final Clock clock;

    public CloseTrialWindowService(CompanyTrialWindowRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyTrialWindowDto execute(Long companyId) {
        CompanyTrialWindow window = repository.findOpenByCompanyId(companyId)
                .orElseThrow(() -> new CompanyTrialWindowNotFoundException(companyId));
        return CompanyTrialWindowDto.from(repository.save(window.close(LocalDateTime.now(clock))));
    }
}
