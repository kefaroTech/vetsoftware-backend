package com.vetsoftware.app.documentwithholding.application.port.in;

import com.vetsoftware.app.documentwithholding.application.dto.DocumentWithholdingDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDocumentWithholdingsUseCase {

    /**
     * Las retenciones de una empresa. No hay hermano sin acotar en este puerto: un
     * listado que no filtra por empresa devuelve filas de todos los tenants
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}). El barrido cross-tenant vive
     * aparte, en {@link ListAllDocumentWithholdingsUseCase}, cerrado a plataforma.
     *
     * <p>
     * <strong>Acotar por la factura de cobro no habria contado como filtro</strong>
     * y por eso no existe esa variante: el documento es de alguien, igual que el
     * certificado, y BE-29 rechaza expresamente la FK ajena como sustituto del
     * tenant.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('documentWithholding.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<DocumentWithholdingDto> listByCompany(Long companyId, int page, int pageSize);
}
