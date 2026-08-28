package com.vetsoftware.app.documentwithholding.application.port.out;

import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 *
 * <p>
 * Y <strong>ninguna escritura salvo {@code save}</strong>: la tabla solo se
 * agrega. No hay {@code delete} y no hay reactivacion, porque una retencion no
 * se desactiva —se corrige con otra fila—. El unico {@code UPDATE} posible es
 * el de {@code certificate_id}, y llega por el mismo {@code save} sobre una
 * entidad ya identificada, para que el {@code @Version} de la fila haga su
 * trabajo.
 */
public interface DocumentWithholdingRepository {

    DocumentWithholding save(DocumentWithholding withholding);

    Optional<DocumentWithholding> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<DocumentWithholding> findAllByCompanyId(Long companyId, int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<DocumentWithholding> findAll(int page, int pageSize);

    /**
     * Lo retenido en un ano y aun sin respaldo, en todas las empresas.
     *
     * <p>
     * Sin filtro de tenant a proposito: es la consulta de vigilancia de tesoreria.
     * Por eso solo la alcanza {@code ListUncertifiedDocumentWithholdingsUseCase},
     * cerrado a {@code hasRole('SYSTEM')} a secas.
     */
    PageResult<DocumentWithholding> findAllUncertifiedByFiscalYear(int fiscalYear, int page,
            int pageSize);

    /** La misma vigilancia acotada a una empresa, que es la que ve el cliente. */
    PageResult<DocumentWithholding> findAllUncertifiedByCompanyIdAndFiscalYear(Long companyId,
            int fiscalYear, int page, int pageSize);
}
