package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageCompanyRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExternalInvoicingOutageCompanyRepository
        implements
            ExternalInvoicingOutageCompanyRepository {

    private final ExternalInvoicingOutageCompanyJpaRepository jpaRepository;
    private final ExternalInvoicingOutageJpaRepository outageJpaRepository;
    private final ExternalInvoicingOutageCompanyJpaMapper mapper;

    public JpaExternalInvoicingOutageCompanyRepository(
            ExternalInvoicingOutageCompanyJpaRepository jpaRepository,
            ExternalInvoicingOutageJpaRepository outageJpaRepository,
            ExternalInvoicingOutageCompanyJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.outageJpaRepository = outageJpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code getReferenceById} y no {@code findById}</strong>: la puente
     * solo necesita la clave foranea, asi que el proxy sin {@code SELECT} es
     * exactamente lo que hace falta. Que la caida existe ya lo comprobo el caso de
     * uso, que es donde ese fallo se puede contar como «esa caida no existe» en vez
     * de como un error de integridad.
     */
    @Override
    public ExternalInvoicingOutageCompany save(ExternalInvoicingOutageCompany affected) {
        ExternalInvoicingOutageJpaEntity outage = outageJpaRepository
                .getReferenceById(affected.getOutageId());
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(affected, outage)));
    }

    @Override
    public boolean existsByOutageIdAndCompanyId(Long outageId, Long companyId) {
        return jpaRepository.existsByOutage_IdAndCompanyId(outageId, companyId);
    }

    @Override
    public PageResult<ExternalInvoicingOutageCompany> findAllByOutageId(Long outageId, int page,
            int pageSize) {
        return Pages.result(
                jpaRepository.findByOutage_Id(outageId, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    /**
     * Las que mas documentos perdieron primero —que es el orden en que se atiende
     * una caida— con el {@code id} de desempate: varias clinicas empatan a cero
     * documentos fallidos con toda normalidad, y sin criterio estable dos paginas
     * consecutivas repetirian u omitirian filas.
     */
    private static Sort order() {
        return Sort.by(Sort.Order.desc("failedDocumentCount"), Sort.Order.asc("id"));
    }
}
