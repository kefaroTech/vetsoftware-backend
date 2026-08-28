package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListExternalInvoicingOutagesUseCase {

    /**
     * El historico de caidas, paginado y de la mas reciente a la mas antigua.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas: es un listado que no filtra por
     * empresa</strong> ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, regla dura,
     * BE-29). Aqui ni siquiera hay empresa por la que filtrar —la tabla es global—,
     * pero la conclusion es la misma y ademas la correcta de negocio: el contador
     * de alcanzadas y las fichas de las demas clinicas no salen por ningun puerto
     * de cliente.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ExternalInvoicingOutageDto> listAll(int page, int pageSize);
}
