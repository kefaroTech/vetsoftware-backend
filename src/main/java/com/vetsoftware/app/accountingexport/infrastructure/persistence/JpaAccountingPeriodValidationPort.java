package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import com.vetsoftware.app.accountingexport.application.port.out.AccountingPeriodValidationPort;
import com.vetsoftware.app.accountingperiod.infrastructure.persistence.AccountingPeriodJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code accountingperiod}.
 *
 * <p>
 * Se apoya en {@code existsByPeriodKey}, que ya existia, y no lee un solo
 * getter de la entidad ajena: a esta exportacion no le hace falta ningun campo
 * del periodo —se archiva bajo su clave— y depender de la forma de una entidad
 * de otra feature es como un cambio inocente alli rompe esto.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features.
 */
@Component("accountingExportJpaAccountingPeriodValidationPort")
public class JpaAccountingPeriodValidationPort implements AccountingPeriodValidationPort {

    private final AccountingPeriodJpaRepository accountingPeriodJpaRepository;

    public JpaAccountingPeriodValidationPort(
            AccountingPeriodJpaRepository accountingPeriodJpaRepository) {
        this.accountingPeriodJpaRepository = accountingPeriodJpaRepository;
    }

    @Override
    public boolean existsByPeriodKey(String periodKey) {
        return periodKey != null && accountingPeriodJpaRepository.existsByPeriodKey(periodKey);
    }
}
