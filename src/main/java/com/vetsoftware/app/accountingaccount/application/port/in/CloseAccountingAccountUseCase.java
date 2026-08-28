package com.vetsoftware.app.accountingaccount.application.port.in;

import com.vetsoftware.app.accountingaccount.application.command.CloseAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CloseAccountingAccountUseCase {

    /**
     * Pone fecha de fin a la vigencia de una cuenta. Es lo que sustituye al
     * borrado: las tres claves foraneas de {@code account_mappings} son
     * {@code RESTRICT} y los asientos ya hechos necesitan que la cuenta siga
     * existiendo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingAccountDto execute(CloseAccountingAccountCommand command);
}
