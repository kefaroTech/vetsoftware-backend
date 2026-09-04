package com.vetsoftware.app.accountingaccount.application.port.in;

import com.vetsoftware.app.accountingaccount.application.command.CreateAccountingAccountCommand;
import com.vetsoftware.app.accountingaccount.application.dto.AccountingAccountDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateAccountingAccountUseCase {

    /**
     * Da de alta una cuenta del plan contable propio.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> Estos son los libros de Lumbre: no hay
     * empresa a la que acotar y no hay nada que un tenant deba poder escribir ni
     * leer aqui. Toda la feature comparte ese gate, que es lo que exige
     * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}: una {@code hasAuthority} suelta
     * en un catalogo global es un endpoint que se abre sembrando un permiso.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingAccountDto execute(CreateAccountingAccountCommand command);
}
