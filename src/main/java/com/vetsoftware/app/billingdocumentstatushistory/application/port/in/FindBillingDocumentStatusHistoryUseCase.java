package com.vetsoftware.app.billingdocumentstatushistory.application.port.in;

import com.vetsoftware.app.billingdocumentstatushistory.application.dto.BillingDocumentStatusHistoryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindBillingDocumentStatusHistoryUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. La
     * variante ancha no existe a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     *
     * <p>
     * El fotograma de otra empresa sale como <strong>no encontrado</strong> y no
     * como prohibido: un 403 confirmaria que la fila existe, y con ids consecutivos
     * eso es un censo de los movimientos de cartera de la competencia.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('billingDocumentStatusHistory.read')"
            + " and @authz.isMyCompany(#companyId))")
    BillingDocumentStatusHistoryDto findById(Long id, Long companyId);
}
