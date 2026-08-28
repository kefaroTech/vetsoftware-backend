package com.vetsoftware.app.accountingperiod.infrastructure.persistence;

import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAccountingPeriodRepository implements AccountingPeriodRepository {

    private final AccountingPeriodJpaRepository jpaRepository;
    private final AccountingPeriodJpaMapper mapper;

    public JpaAccountingPeriodRepository(AccountingPeriodJpaRepository jpaRepository,
            AccountingPeriodJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public AccountingPeriod save(AccountingPeriod period) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(period)));
    }

    @Override
    public Optional<AccountingPeriod> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByPeriodKey(AccountingPeriodKey periodKey) {
        return jpaRepository.existsByPeriodKey(periodKey.value());
    }

    @Override
    public long countOpenExcluding(Long excludedId) {
        return jpaRepository.countByStatusAndIdNot(AccountingPeriodStatus.OPEN, excludedId);
    }

    /**
     * <strong>El {@code >=} lo resuelve el motor sobre {@code CHAR(7)} con colacion
     * {@code ascii_bin}</strong>, es decir byte a byte, que sobre {@code yyyy-MM}
     * es el orden del calendario. Por eso no hace falta convertir la clave a fecha
     * ni traerse los periodos a memoria para compararlos: la misma consulta
     * resuelve las dos ramas de la regla de imputacion —el mes exacto si esta
     * abierto, el siguiente abierto si no—.
     */
    @Override
    public Optional<AccountingPeriod> findFirstOpenFrom(AccountingPeriodKey periodKey) {
        return jpaRepository.findFirstByStatusAndPeriodKeyGreaterThanEqual(
                AccountingPeriodStatus.OPEN, periodKey.value(), masAntiguo()).map(mapper::toDomain);
    }

    @Override
    public PageResult<AccountingPeriod> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, masReciente())),
                mapper::toDomain);
    }

    /**
     * Calendario completo: el mes mas reciente primero, que es como se mira un
     * calendario contable —lo que interesa es si el mes que acaba de terminar ya se
     * cerro—. Desempate por {@code id} descendente para que el orden sea total: hoy
     * {@code uq_accounting_periods_period} hace imposible el empate, pero el
     * desempate cuesta una linea y su ausencia solo se nota cuando dos paginas
     * consecutivas repiten u omiten un mes.
     */
    private static Sort masReciente() {
        return Sort.by(Sort.Direction.DESC, "periodKey").and(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Resolucion del periodo de imputacion: <strong>ascendente</strong>, al reves
     * que el listado y a proposito. La consulta pide «la primera clave mayor o
     * igual», y con orden descendente esa primera fila seria la <em>ultima</em> del
     * calendario: un hecho de marzo acabaria imputado al ultimo mes abierto del ano
     * en vez de al primero posterior a marzo, sin error y sin log.
     */
    private static Sort masAntiguo() {
        return Sort.by(Sort.Direction.ASC, "periodKey").and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
