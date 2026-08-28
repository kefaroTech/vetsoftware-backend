package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindExternalInvoicingOutageUseCase {

    /**
     * Una caida por su identificador.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y el puerto no recibe
     * {@code companyId} porque la tabla no tiene esa columna.</strong> Esa es
     * exactamente la forma que {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}
     * exige cerrar a plataforma: un {@code id} lo escribe el cliente en la URL, y
     * sin empresa que acotar la unica autorizacion honesta es la ancha.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    ExternalInvoicingOutageDto execute(Long id);
}
