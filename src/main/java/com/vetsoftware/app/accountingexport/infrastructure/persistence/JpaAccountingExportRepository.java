package com.vetsoftware.app.accountingexport.infrastructure.persistence;

import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.domain.AccountingExport;
import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAccountingExportRepository implements AccountingExportRepository {

    private final AccountingExportJpaRepository jpaRepository;
    private final AccountingExportJpaMapper mapper;

    public JpaAccountingExportRepository(AccountingExportJpaRepository jpaRepository,
            AccountingExportJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}.</strong> Spring Data no
     * incrementa {@code @Version} hasta el flush, y aqui el flush hace ademas que
     * las dos unicidades que importan —{@code uq_accounting_exports_attempt} y
     * {@code uq_accounting_exports_current}— salgan <em>dentro</em> del caso de uso
     * y no al cerrar la transaccion, donde ya no hay quien las traduzca. Es el
     * mismo defecto que mordio en {@code JpaDocumentWithholdingRepository}.
     */
    @Override
    public AccountingExport save(AccountingExport export) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(export)));
    }

    @Override
    public Optional<AccountingExport> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<AccountingExport> findAllByPeriodKey(String periodKey, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByPeriodKey(periodKey,
                Pages.request(page, pageSize, trayOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<AccountingExport> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, sweepOrder())),
                mapper::toDomain);
    }

    @Override
    public Optional<Integer> findLastAttemptNumber(String periodKey,
            AccountingExportKind exportKind) {
        return jpaRepository
                .findFirstByPeriodKeyAndExportKindOrderByAttemptNumberDesc(periodKey, exportKind)
                .map(AccountingExportJpaEntity::getAttemptNumber);
    }

    /**
     * La bandeja de un mes: por clase de fichero y, dentro de cada una, el intento
     * mas alto primero. El {@code id} desempata; sin un criterio estable dos
     * paginas consecutivas pueden repetir u omitir filas.
     */
    private static Sort trayOrder() {
        return Sort.by(Sort.Order.asc("exportKind"), Sort.Order.desc("attemptNumber"),
                Sort.Order.desc("id"));
    }

    /**
     * El barrido de plataforma: lo mas recien generado primero. Es el orden al que
     * sirve {@code ix_accounting_exports_generated}.
     */
    private static Sort sweepOrder() {
        return Sort.by(Sort.Order.desc("generatedAt"), Sort.Order.desc("id"));
    }
}
