package com.vetsoftware.app.billingdocumentstatushistory.application.port.in;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La pelicula de un documento de cobro: todos sus cambios de estado, en orden.
 */
public interface ListBillingDocumentStatusHistoryUseCase {

    /**
     * <strong>Recibe las dos columnas y no solo el documento.</strong> Acotar por
     * {@code billingDocumentId} a secas parece suficiente —el documento es de una
     * empresa— y es justo el error que persigue
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29): la FK ajena no sustituye al
     * filtro de tenant, porque quien escribe el id en la URL es el cliente y nadie
     * habria comprobado de quien es ese documento.
     *
     * <p>
     * Ademas es el orden literal de {@code ix_bdsh_document}, asi que el filtro y
     * la ordenacion los sirve el mismo indice.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('billingDocumentStatusHistory.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<BillingDocumentStatusHistoryDto> listByDocument(Long companyId,
            Long billingDocumentId, int page, int pageSize);
}
