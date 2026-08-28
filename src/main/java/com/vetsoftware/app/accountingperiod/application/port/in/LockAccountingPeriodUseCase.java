package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.command.LockAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface LockAccountingPeriodUseCase {

    /**
     * Declara el mes: pasa a {@code LOCKED} y deja de ser reabrible para siempre.
     *
     * <p>
     * <strong>Es la operacion menos reversible de todo el modelo</strong> —no hay
     * ningun camino de vuelta desde {@code LOCKED}, ni con firma ni con motivo— y
     * aun asi va con el mismo {@code hasRole('SYSTEM')} a secas que el resto de la
     * feature, por lo mismo que se explica en
     * {@link SoftCloseAccountingPeriodUseCase}: el rol de contador externo que la
     * regla de negocio nombra no existe todavia en este backend. Cuando exista, el
     * estrechamiento empieza por esta expresion.
     *
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException
     *             si el periodo no existe
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodAlreadyClosedException
     *             si ya estaba declarado
     * @throws com.vetsoftware.app.accountingperiod.domain.LastOpenAccountingPeriodException
     *             si es el ultimo periodo abierto
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingPeriodDto execute(LockAccountingPeriodCommand command);
}
