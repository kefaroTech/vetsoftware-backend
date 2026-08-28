package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExternalInvoiceReconciliationRepository
        implements
            ExternalInvoiceReconciliationRepository {

    private final ExternalInvoiceReconciliationJpaRepository jpaRepository;
    private final ExternalInvoiceReconciliationJpaMapper mapper;

    public JpaExternalInvoiceReconciliationRepository(
            ExternalInvoiceReconciliationJpaRepository jpaRepository,
            ExternalInvoiceReconciliationJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ExternalInvoiceReconciliation save(ExternalInvoiceReconciliation reconciliation) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(reconciliation)));
    }

    @Override
    public Optional<ExternalInvoiceReconciliation> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCompanyIdAndBillingDocumentId(Long companyId, Long billingDocumentId) {
        return companyId != null && billingDocumentId != null && jpaRepository
                .existsByCompanyIdAndBillingDocumentId(companyId, billingDocumentId);
    }

    @Override
    public PageResult<ExternalInvoiceReconciliation> findAll(int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAll(Pages.request(page, pageSize, recientesPrimero())),
                mapper::toDomain);
    }

    @Override
    public PageResult<ExternalInvoiceReconciliation> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, recientesPrimero())), mapper::toDomain);
    }

    @Override
    public PageResult<ExternalInvoiceReconciliation> findAllByStatus(
            ExternalInvoiceReconciliationStatus status, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByStatus(status,
                Pages.request(page, pageSize, antiguasPrimero())), mapper::toDomain);
    }

    /**
     * Orden total y estable del barrido de la consola: lo ultimo abierto arriba,
     * con el {@code id} de desempate. Sin ese desempate, dos conciliaciones
     * abiertas en el mismo microsegundo -las de un cierre de mes generadas en lote,
     * que es el caso normal- pueden salir en dos paginas o en ninguna.
     */
    private static Sort recientesPrimero() {
        return Sort.by(Sort.Direction.DESC, "createdDate").and(Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Orden de la bandeja de lo que nadie facturo: <strong>al reves que el
     * barrido</strong>, y a proposito. Lo que lleva mas dias devengado sin factura
     * externa es lo primero que hay que mirar; ponerlo al final es como se pierde
     * de vista. Ademas es el orden natural de {@code ix_eir_pending (status,
     * created_date)}, asi que la bandeja se sirve del indice y no de un
     * {@code filesort}.
     */
    private static Sort antiguasPrimero() {
        return Sort.by(Sort.Direction.ASC, "createdDate").and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
