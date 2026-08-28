package com.vetsoftware.app.billingdocumentstatushistory.infrastructure.persistence;

import com.vetsoftware.app.billingdocumentstatushistory.application.port.out.BillingDocumentStatusHistoryRepository;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBillingDocumentStatusHistoryRepository
        implements
            BillingDocumentStatusHistoryRepository {

    private final BillingDocumentStatusHistoryJpaRepository jpaRepository;
    private final BillingDocumentStatusHistoryJpaMapper mapper;

    public JpaBillingDocumentStatusHistoryRepository(
            BillingDocumentStatusHistoryJpaRepository jpaRepository,
            BillingDocumentStatusHistoryJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public BillingDocumentStatusHistory save(BillingDocumentStatusHistory entry) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(entry)));
    }

    @Override
    public Optional<BillingDocumentStatusHistory> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<BillingDocumentStatusHistory> findAllByCompanyIdAndBillingDocumentId(
            Long companyId, Long billingDocumentId, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyIdAndBillingDocumentId(companyId,
                billingDocumentId, Pages.request(page, pageSize, filmOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<BillingDocumentStatusHistory> findAllByCompanyIdAndToStatus(Long companyId,
            BillingDocumentStatus toStatus, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyIdAndToStatus(companyId, toStatus,
                Pages.request(page, pageSize, latestFirstOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<BillingDocumentStatusHistory> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, latestFirstOrder())), mapper::toDomain);
    }

    @Override
    public PageResult<BillingDocumentStatusHistory> findAll(int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAll(Pages.request(page, pageSize, latestFirstOrder())),
                mapper::toDomain);
    }

    /**
     * Orden de proyeccion de la pelicula: <strong>ascendente</strong>, del primer
     * fotograma al ultimo, que es como se lee una historia y como la deja ya
     * ordenada {@code ix_bdsh_document}.
     *
     * <p>
     * El desempate por {@code id} no es adorno: varios movimientos del mismo
     * documento pueden compartir {@code occurred_at} cuando los escribe el proceso
     * automatico en la misma transaccion, y sin un orden total dos paginas
     * consecutivas repiten u omiten filas. Una bitacora que pierde fotogramas al
     * paginar es peor que no tenerla, porque nadie sabe que falta. Ascendente
     * tambien en el desempate, para que empatados salgan en el orden en que se
     * escribieron.
     */
    private static Sort filmOrder() {
        return Sort.by(Sort.Direction.ASC, "occurredAt").and(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Orden de bandeja: lo mas reciente primero, con el {@code id} de desempate. Es
     * el orden util cuando lo que se mira no es la historia de un documento sino
     * que se ha movido ultimamente en toda la empresa.
     */
    private static Sort latestFirstOrder() {
        return Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
