package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindDocumentWithholdingUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     *
     * <p>
     * El tenant llega aqui porque <strong>la retencion de un cliente es
     * suya</strong>: es plata propia que fue a la DIAN a su nombre, y es el unico
     * que la puede imputar en su declaracion. Negarle verla seria esconderle su
     * propio anticipo de impuesto.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('documentWithholding.read')"
            + " and @authz.isMyCompany(#companyId))")
    DocumentWithholdingDto findById(Long id, Long companyId);
}
