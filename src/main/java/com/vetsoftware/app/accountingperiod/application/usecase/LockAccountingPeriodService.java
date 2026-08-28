package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.command.LockAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.LockAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException;
import com.vetsoftware.app.accountingperiod.domain.LastOpenAccountingPeriodException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Declara un mes: {@code LOCKED}, sin vuelta atras.
 *
 * <p>
 * <strong>Es un service aparte del que cierra en blando aunque el codigo se
 * parezca.</strong> Son dos decisiones distintas de negocio —«cerrado y
 * corregible» y «declarado y definitivo»— y el CLAUDE.md pide un service por
 * caso de uso justamente para que la segunda no herede en silencio los cambios
 * de la primera.
 *
 * <p>
 * <strong>La guarda del ultimo periodo abierto tambien aplica aqui</strong>, y
 * por la misma razon: declarar directamente el unico mes abierto deja al
 * sistema sin donde imputar un hecho tardio, y ademas de forma irreversible.
 * Solo se comprueba si el periodo esta abierto — declarar uno ya cerrado en
 * blando no cambia cuantos quedan abiertos.
 */
@Observed(name = "accounting.period.lock")
@Service
public class LockAccountingPeriodService implements LockAccountingPeriodUseCase {

    private final AccountingPeriodRepository repository;
    private final Clock clock;

    public LockAccountingPeriodService(AccountingPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingPeriodDto execute(LockAccountingPeriodCommand command) {
        AccountingPeriod period = repository.findById(command.id())
                .orElseThrow(() -> new AccountingPeriodNotFoundException(command.id()));
        if (period.acceptsPostings() && repository.countOpenExcluding(command.id()) == 0)
            throw new LastOpenAccountingPeriodException(command.id());

        period.lock(command.systemUserId(), LocalDateTime.now(clock));
        return AccountingPeriodDto.from(repository.save(period));
    }
}
