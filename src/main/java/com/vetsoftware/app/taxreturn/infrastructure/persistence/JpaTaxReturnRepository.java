package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.taxreturn.application.port.out.TaxReturnRepository;
import com.vetsoftware.app.taxreturn.domain.TaxReturn;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaxReturnRepository implements TaxReturnRepository {

    private final TaxReturnJpaRepository jpaRepository;
    private final TaxReturnJpaMapper mapper;

    public JpaTaxReturnRepository(TaxReturnJpaRepository jpaRepository, TaxReturnJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}, y aqui es
     * load-bearing.</strong> La correccion escribe dos filas en la misma
     * transaccion: primero la anterior pasa a {@code CORRECTED} —lo que vacia su
     * {@code current_return_marker}— y solo entonces cabe la nueva. Sin flush,
     * Hibernate podria reordenar las dos sentencias y el {@code INSERT} chocaria
     * contra {@code uq_tax_returns_current} por un motivo que no tiene nada que ver
     * con el negocio.
     */
    @Override
    public TaxReturn save(TaxReturn taxReturn) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(taxReturn)));
    }

    @Override
    public Optional<TaxReturn> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<TaxReturn> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, ledgerOrder())),
                mapper::toDomain);
    }

    @Override
    public PageResult<TaxReturn> findAllByFiscalPeriodKey(String fiscalPeriodKey, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByFiscalPeriodKey(fiscalPeriodKey,
                Pages.request(page, pageSize, periodOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<TaxReturn> findAllByFirmezaUntilBefore(LocalDate limit, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByFirmezaUntilBefore(limit,
                Pages.request(page, pageSize, firmezaOrder())), mapper::toDomain);
    }

    /**
     * El archivo se lee del periodo mas reciente hacia atras y, dentro de cada uno,
     * de la correccion mas alta a la inicial. El {@code id} desempata porque
     * {@code uq_tax_returns_case} hace imposible repetir el trio hoy, pero un orden
     * que depende de una constraint que un changeset futuro puede mover repite u
     * omite filas entre dos paginas consecutivas.
     */
    private static Sort ledgerOrder() {
        return Sort.by(Sort.Order.desc("fiscalYear"), Sort.Order.desc("fiscalPeriodKey"),
                Sort.Order.asc("taxKind"), Sort.Order.desc("sequenceNumber"),
                Sort.Order.desc("id"));
    }

    /** Dentro de un periodo: por impuesto, y la correccion mas alta primero. */
    private static Sort periodOrder() {
        return Sort.by(Sort.Order.asc("taxKind"), Sort.Order.asc("municipalityCode"),
                Sort.Order.desc("sequenceNumber"), Sort.Order.desc("id"));
    }

    /**
     * El barrido de conservacion: lo que queda en firme antes, primero. Es el orden
     * al que sirve {@code ix_tax_returns_firmeza}.
     */
    private static Sort firmezaOrder() {
        return Sort.by(Sort.Order.asc("firmezaUntil"), Sort.Order.asc("id"));
    }
}
