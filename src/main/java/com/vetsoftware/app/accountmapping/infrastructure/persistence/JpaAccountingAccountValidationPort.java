package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import com.vetsoftware.app.accountingaccount.infrastructure.persistence.AccountingAccountJpaRepository;
import com.vetsoftware.app.accountmapping.application.port.out.AccountingAccountValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code accountingaccount}.
 *
 * <p>
 * Se apoya en dos consultas derivadas y no lee un solo getter de la entidad
 * ajena: a este mapeo no le hace falta ningun campo de la cuenta —se archiva
 * bajo su codigo— y depender de la forma de una entidad de otra feature es como
 * un cambio inocente alli rompe esto.
 *
 * <p>
 * <strong>Los metodos derivados viven en {@code AccountingAccountJpaRepository}
 * y no aqui</strong>, por la misma coordinacion que documenta
 * {@code withholdingraterule.JpaMunicipalityValidationPort}: el
 * {@code XxxJpaRepository} de una feature se declara una sola vez, asi que dos
 * slices anadiendole el mismo metodo serian un conflicto de escritura sobre ese
 * archivo y no dos metodos.
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features.
 */
@Component("accountMappingJpaAccountingAccountValidationPort")
public class JpaAccountingAccountValidationPort implements AccountingAccountValidationPort {

    private final AccountingAccountJpaRepository accountingAccountJpaRepository;

    public JpaAccountingAccountValidationPort(
            AccountingAccountJpaRepository accountingAccountJpaRepository) {
        this.accountingAccountJpaRepository = accountingAccountJpaRepository;
    }

    @Override
    public boolean existsByCode(String code) {
        return code != null && accountingAccountJpaRepository.existsByCode(code);
    }

    @Override
    public boolean existsPostableByCode(String code) {
        return code != null && accountingAccountJpaRepository.existsByCodeAndPostableTrue(code);
    }
}
