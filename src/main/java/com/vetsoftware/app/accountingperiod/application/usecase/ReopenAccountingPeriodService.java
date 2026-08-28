package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.command.ReopenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.ReopenAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reabre un mes cerrado, con firma y motivo escrito.
 *
 * <p>
 * <strong>No hay guarda de «ultimo periodo abierto» aqui</strong>, y no es una
 * omision: reabrir <em>suma</em> un mes abierto, nunca resta. La invariante que
 * protege esa guarda no puede romperse por este camino.
 *
 * <p>
 * <strong>Las tres reglas de la reapertura viven en el dominio</strong> —solo
 * desde {@code SOFT_CLOSED}, nunca desde {@code LOCKED}, motivo obligatorio y
 * {@code reopenedAt >= closedAt}— porque las tres se contestan mirando
 * unicamente esta fila. Este servicio solo aporta la hora del reloj inyectado y
 * la transaccion.
 *
 * <p>
 * <strong>La hora sale del reloj inyectado, nunca de un
 * {@code LocalDateTime.now()} pelado.</strong> No es solo determinismo de test:
 * {@code ClockConfig} fija la zona del negocio en {@code America/Bogota} y la
 * JVM de produccion corre en UTC. Una reapertura hecha a las 19:30 quedaria
 * fechada al dia siguiente, y {@code reopened_at} es justo lo que un auditor
 * usa para reconstruir el orden de los hechos.
 */
@Observed(name = "accounting.period.reopen")
@Service
public class ReopenAccountingPeriodService implements ReopenAccountingPeriodUseCase {

    private final AccountingPeriodRepository repository;
    private final Clock clock;

    public ReopenAccountingPeriodService(AccountingPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingPeriodDto execute(ReopenAccountingPeriodCommand command) {
        AccountingPeriod period = repository.findById(command.id())
                .orElseThrow(() -> new AccountingPeriodNotFoundException(command.id()));
        period.reopen(command.systemUserId(), LocalDateTime.now(clock), command.reason());
        return AccountingPeriodDto.from(repository.save(period));
    }
}
