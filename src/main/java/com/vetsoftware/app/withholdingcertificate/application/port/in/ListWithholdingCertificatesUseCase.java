package com.vetsoftware.app.withholdingcertificate.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.withholdingcertificate.application.dto.WithholdingCertificateDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListWithholdingCertificatesUseCase {

    /**
     * Los certificados de una empresa. No hay hermano sin acotar en este puerto: un
     * listado que no filtra por empresa devuelve filas de todos los tenants
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). El barrido cross-tenant vive
     * aparte, en {@link ListAllWithholdingCertificatesUseCase}, cerrado a
     * plataforma.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('withholdingCertificate.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<WithholdingCertificateDto> listByCompany(Long companyId, int page, int pageSize);
}
