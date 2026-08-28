package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.command.SoftCloseAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.SoftCloseAccountingPeriodUseCase;
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
 * Cierra un mes dejandolo corregible.
 *
 * <p>
 * <strong>Aqui vive «tiene que existir siempre al menos un periodo
 * abierto»</strong>, y no en el constructor de la entidad: es una invariante
 * del conjunto de periodos, y un agregado solo puede hablar de si mismo. Cerrar
 * el ultimo abierto dejaria a {@code ResolvePostingPeriodUseCase} sin donde
 * imputar nada, y el sintoma no aparece en el cierre sino al dia siguiente y en
 * otra feature.
 *
 * <p>
 * <strong>La comprobacion solo se hace si el periodo esta abierto</strong>: si
 * ya estaba cerrado, cerrarlo otra vez no reduce el numero de meses abiertos y
 * quien tiene que contestar es el dominio con
 * {@code AccountingPeriodAlreadyClosedException}. Al reves —contar primero y
 * preguntar despues— daria «es el ultimo abierto» sobre un mes que no lo esta.
 *
 * <p>
 * <strong>{@code @Transactional} porque son tres operaciones</strong> —cargar,
 * contar y guardar— y porque entre ellas vive la decision: sin la transaccion,
 * el {@code @Version} de la entidad no tendria donde comparar y el empate
 * exacto de dos cierres se resolveria por el ultimo que escribe.
 */
@Observed(name = "accounting.period.soft.close")
@Service
public class SoftCloseAccountingPeriodService implements SoftCloseAccountingPeriodUseCase {

    private final AccountingPeriodRepository repository;
    private final Clock clock;

    public SoftCloseAccountingPeriodService(AccountingPeriodRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AccountingPeriodDto execute(SoftCloseAccountingPeriodCommand command) {
        AccountingPeriod period = repository.findById(command.id())
                .orElseThrow(() -> new AccountingPeriodNotFoundException(command.id()));
        if (period.acceptsPostings() && repository.countOpenExcluding(command.id()) == 0)
            throw new LastOpenAccountingPeriodException(command.id());

        period.softClose(command.systemUserId(), LocalDateTime.now(clock));
        return AccountingPeriodDto.from(repository.save(period));
    }
}
