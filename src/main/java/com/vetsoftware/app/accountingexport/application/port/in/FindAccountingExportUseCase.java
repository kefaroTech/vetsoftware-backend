package com.vetsoftware.app.accountingexport.application.port.in;

import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindAccountingExportUseCase {

    /** Una exportacion por su id. */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingExportDto findById(Long id);
}
