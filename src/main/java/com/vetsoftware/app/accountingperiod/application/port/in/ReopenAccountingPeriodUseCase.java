package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.command.ReopenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReopenAccountingPeriodUseCase {

    /**
     * Vuelve a abrir un mes {@code SOFT_CLOSED}, con firma y motivo escrito.
     *
     * <h2>El gate es MAS ANCHO de lo que la regla de negocio pide</h2>
     *
     * <p>
     * La regla dice «solo el contador externo cierra y reabre», y esta es la
     * operacion en la que mas se nota: reabrir un mes es deshacer un cierre que ya
     * se comunico hacia dentro de la empresa. <strong>El rol de contador externo no
     * existe en este backend</strong> —hay un unico rol, {@code ROLE_SYSTEM}—, la
     * tabla no tiene {@code company_id} con el que acotar, y
     * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM} prohibe abrir por
     * {@code hasAuthority} suelto lo que la feature cierra a SYSTEM. Asi que el
     * gate queda a {@code hasRole('SYSTEM')} a secas: cualquier cuenta de
     * plataforma puede reabrir.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que el rol llegue.</strong> El
     * estrechamiento va en esta expresion y en la de
     * {@link SoftCloseAccountingPeriodUseCase}, no en el controller ni en un
     * filtro: el {@code @PreAuthorize} del puerto es lo unico que protege tambien
     * al caller que no pasa por HTTP. Mientras tanto, lo que queda es el rastro:
     * {@code reopened_by_system_user_id} y {@code reopened_reason} —obligatorio—
     * dicen quien reabrio y por que.
     *
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException
     *             si el periodo no existe
     * @throws com.vetsoftware.app.accountingperiod.domain.LockedAccountingPeriodCannotBeReopenedException
     *             si el periodo esta {@code LOCKED}
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotClosedException
     *             si el periodo no estaba cerrado
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingPeriodDto execute(ReopenAccountingPeriodCommand command);
}
