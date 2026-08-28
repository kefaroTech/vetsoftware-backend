package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import com.vetsoftware.app.documentwithholding.application.port.out.DocumentWithholdingRepository;
import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDocumentWithholdingRepository implements DocumentWithholdingRepository {

    private final DocumentWithholdingJpaRepository jpaRepository;
    private final DocumentWithholdingJpaMapper mapper;

    public JpaDocumentWithholdingRepository(DocumentWithholdingJpaRepository jpaRepository,
            DocumentWithholdingJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}: la version que devuelve tiene
     * que ser la que hay en la base.</strong>
     *
     * <p>
     * Esta tabla solo se agrega, y su unica mutacion —apuntar
     * {@code certificate_id}— la protege {@code @Version}. Con un {@code save}
     * normal, Hibernate encola el {@code UPDATE} y no incrementa la version hasta
     * el flush, asi que el agregado que sale de aqui lleva <em>la version de
     * antes</em>: quien lo reutilice para una segunda escritura en la misma
     * transaccion enviaria una version rancia, y el chequeo optimista que se cree
     * activo no compara nada util. Devolver la fila ya escrita es la misma razon
     * por la que este metodo devuelve el {@code id} generado, una linea mas arriba
     * en el mismo contrato.
     */
    @Override
    public DocumentWithholding save(DocumentWithholding withholding) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(withholding)));
    }

    @Override
    public Optional<DocumentWithholding> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public PageResult<DocumentWithholding> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyId(companyId, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    @Override
    public PageResult<DocumentWithholding> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    @Override
    public PageResult<DocumentWithholding> findAllUncertifiedByFiscalYear(int fiscalYear, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByFiscalYearAndCertificateIdIsNull(
                (short) fiscalYear, Pages.request(page, pageSize, order())), mapper::toDomain);
    }

    @Override
    public PageResult<DocumentWithholding> findAllUncertifiedByCompanyIdAndFiscalYear(
            Long companyId, int fiscalYear, int page, int pageSize) {
        return Pages.result(
                jpaRepository.findAllByCompanyIdAndFiscalYearAndCertificateIdIsNull(companyId,
                        (short) fiscalYear, Pages.request(page, pageSize, order())),
                mapper::toDomain);
    }

    /**
     * Orden total y estable: lo mas reciente primero por fecha de practica, con el
     * {@code id} de desempate. Sin desempate, dos retenciones del mismo dia —lo
     * normal, porque una factura puede llevar retefuente, reteiva y reteica a la
     * vez— pueden salir en dos paginas o en ninguna. Y una bandeja de reclamacion
     * que pierde filas al paginar es peor que no tenerla: nadie sabe que falta.
     */
    private static Sort order() {
        return Sort.by(Sort.Direction.DESC, "practicedOn").and(Sort.by(Sort.Direction.DESC, "id"));
    }
}
