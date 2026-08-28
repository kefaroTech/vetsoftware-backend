package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindWithholdingCertificateUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida
     * ({@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}, BE-COV).
     *
     * <p>
     * El tenant llega aqui porque el certificado de un cliente es suyo: la
     * retencion se la practicaron a el y es el quien la imputa. La plataforma
     * escribe, los dos leen.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('withholdingCertificate.read')"
            + " and @authz.isMyCompany(#companyId))")
    WithholdingCertificateDto findById(Long id, Long companyId);
}
