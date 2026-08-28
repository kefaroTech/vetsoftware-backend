package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.command.OpenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OpenAccountingPeriodUseCase {

    /**
     * Abre un mes contable.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision, no un olvido.</strong> El calendario contable es de la
     * plataforma: si cada clinica abriera y cerrara sus propios meses, la misma
     * conciliacion podria quedar imputada a marzo para una y a abril para otra, y
     * el informe consolidado dejaria de cuadrar sin que nada fallara. La tabla no
     * tiene {@code company_id}, asi que tampoco existe el {@code companyId} con el
     * que acotar.
     *
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodAlreadyExistsException
     *             si ese mes ya se abrio
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingPeriodDto execute(OpenAccountingPeriodCommand command);
}
