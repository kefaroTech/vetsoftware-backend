package com.vetsoftware.app.accountingperiod.application.usecase;

import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.in.FindAccountingPeriodUseCase;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "accounting.period.find")
@Service
public class FindAccountingPeriodService implements FindAccountingPeriodUseCase {

    private final AccountingPeriodRepository repository;

    public FindAccountingPeriodService(AccountingPeriodRepository repository) {
        this.repository = repository;
    }

    /**
     * La carga es ancha porque no existe otra: la tabla no tiene empresa. Lo que
     * exime a este servicio de {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} no es una
     * excepcion escrita a mano, es que el puerto de salida no declara ninguna
     * variante acotada que este servicio pudiera estar ignorando.
     */
    @Override
    public AccountingPeriodDto findById(Long id) {
        return repository.findById(id).map(AccountingPeriodDto::from)
                .orElseThrow(() -> new AccountingPeriodNotFoundException(id));
    }
}
