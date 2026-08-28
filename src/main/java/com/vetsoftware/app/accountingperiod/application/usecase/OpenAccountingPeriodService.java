package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.command.OpenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.OpenAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodAlreadyExistsException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre un mes contable.
 *
 * <p>
 * <strong>La comprobacion previa de duplicado no sustituye a la unicidad de la
 * base: la traduce.</strong> {@code uq_accounting_periods_period} sigue siendo
 * lo unico que garantiza que no entren dos, porque entre el {@code exists} y el
 * {@code insert} cabe otra transaccion. Lo que aporta es que el caso comun —el
 * cierre mensual disparado dos veces— conteste un 409 con la clave del mes en
 * el mensaje en vez de un 500 con un {@code Duplicate entry} del driver.
 *
 * <p>
 * <strong>El formato de la clave lo valida el value object, no este
 * servicio.</strong> {@code AccountingPeriodKey.of} es donde vive el espejo del
 * {@code CHECK}; repetir aqui el regex seria un segundo sitio que puede
 * divergir.
 */
@Observed(name = "accounting.period.open")
@Service
public class OpenAccountingPeriodService implements OpenAccountingPeriodUseCase {

    private final AccountingPeriodRepository repository;
    private final Clock clock;

    public OpenAccountingPeriodService(AccountingPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingPeriodDto execute(OpenAccountingPeriodCommand command) {
        AccountingPeriodKey periodKey = AccountingPeriodKey.of(command.periodKey());
        if (repository.existsByPeriodKey(periodKey))
            throw new AccountingPeriodAlreadyExistsException(periodKey);

        AccountingPeriod period = AccountingPeriod.open(periodKey, LocalDateTime.now(clock));
        return AccountingPeriodDto.from(repository.save(period));
    }
}
