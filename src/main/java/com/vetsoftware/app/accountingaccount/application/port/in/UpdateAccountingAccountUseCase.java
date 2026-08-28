package com.vetsoftware.app.accountingaccount.application.port.in;

import com.vetsoftware.app.accountingaccount.application.command.UpdateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdateAccountingAccountUseCase {

    /**
     * Corrige el nombre de la cuenta y si exige tercero identificado. Nada mas: el
     * codigo, la clase, el nivel y el padre definen que <em>significa</em> la
     * cuenta y no se editan.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Senala una fila concreta
     * por su id y no recibe empresa —no la hay—, que es el caso que
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} exige cerrar a plataforma.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingAccountDto execute(UpdateAccountingAccountCommand command);
}
