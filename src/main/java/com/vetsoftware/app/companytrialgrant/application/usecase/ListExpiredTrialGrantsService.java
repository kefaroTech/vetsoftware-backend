package com.vetsoftware.app.companytrialgrant.application.usecase;

import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ListExpiredTrialGrantsUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El barrido de vencimientos. Sirve filas de todas las empresas, así que su
 * puerto está cerrado a un principal cross-tenant.
 */
@Service
public class ListExpiredTrialGrantsService implements ListExpiredTrialGrantsUseCase {

    private final CompanyTrialGrantRepository repository;

    public ListExpiredTrialGrantsService(CompanyTrialGrantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyTrialGrantDto> listLiveExpiredOn(LocalDate day) {
        return repository.findLiveExpiredOn(day).stream().map(CompanyTrialGrantDto::from).toList();
    }
}
