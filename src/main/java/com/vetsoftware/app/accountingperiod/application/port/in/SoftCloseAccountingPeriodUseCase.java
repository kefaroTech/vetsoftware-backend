package com.vetsoftware.app.accountingperiod.application.port.in;

import com.vetsoftware.app.accountingperiod.application.command.SoftCloseAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SoftCloseAccountingPeriodUseCase {

    /**
     * Cierra el mes dejandolo corregible: {@code OPEN} → {@code SOFT_CLOSED}.
     *
     * <h2>El gate es MAS ANCHO de lo que la regla de negocio pide, y esta escrito
     * aqui para el dia que eso se pueda arreglar</h2>
     *
     * <p>
     * La regla de negocio dice «solo el contador externo cierra y reabre».
     * <strong>Ese rol no existe en este backend.</strong> Hoy hay exactamente un
     * rol —{@code ROLE_SYSTEM}— y ningun otro: 855 usos de
     * {@code hasRole('SYSTEM')} y cero de cualquier otro. Ademas la tabla no tiene
     * {@code company_id}, asi que no hay camino de tenant por el que estrechar, y
     * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM} prohibe abrir por un
     * {@code hasAuthority} suelto lo que la feature cierra a SYSTEM —hacerlo seria
     * sembrar un permiso sobre una ficha que comparten todos los tenants—.
     *
     * <p>
     * <strong>Asi que el gate queda a {@code hasRole('SYSTEM')} a secas, que es mas
     * ancho de lo debido: cualquier cuenta de plataforma puede cerrar el
     * mes.</strong> Quien traiga el rol de contador externo no leera el changelog,
     * leera este puerto: el estrechamiento va <em>aqui</em>, en esta expresion y en
     * la de {@link ReopenAccountingPeriodUseCase}, y no en un filtro ni en el
     * controller. Mientras tanto, la firma de quien cerro queda en
     * {@code closed_by_system_user_id}, que es lo que permite auditar despues lo
     * que el gate no acota antes.
     *
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException
     *             si el periodo no existe
     * @throws com.vetsoftware.app.accountingperiod.domain.AccountingPeriodAlreadyClosedException
     *             si ya estaba cerrado o declarado
     * @throws com.vetsoftware.app.accountingperiod.domain.LastOpenAccountingPeriodException
     *             si es el ultimo periodo abierto
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingPeriodDto execute(SoftCloseAccountingPeriodCommand command);
}
