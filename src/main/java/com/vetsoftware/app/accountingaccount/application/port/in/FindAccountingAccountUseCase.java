package com.vetsoftware.app.accountingaccount.application.port.in;

import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindAccountingAccountUseCase {

    /** Una cuenta por su id. */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingAccountDto findById(Long id);

    /**
     * Una cuenta por su codigo, que es como la nombra {@code account_mappings}. Es
     * la consulta con la que la consola comprueba que el codigo que va a mapear
     * existe y admite asiento antes de guardarlo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingAccountDto findByCode(String code);
}
