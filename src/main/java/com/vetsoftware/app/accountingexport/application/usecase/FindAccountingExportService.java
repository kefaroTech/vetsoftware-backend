package com.vetsoftware.app.accountingexport.application.usecase;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.in.FindAccountingExportUseCase;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.domain.AccountingExportNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Una exportacion por su id. */
@Observed(name = "accounting.export.find")
@Service
public class FindAccountingExportService implements FindAccountingExportUseCase {

    private final AccountingExportRepository repository;

    public FindAccountingExportService(AccountingExportRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccountingExportDto findById(Long id) {
        return repository.findById(id).map(AccountingExportDto::from)
                .orElseThrow(() -> new AccountingExportNotFoundException(id));
    }
}
