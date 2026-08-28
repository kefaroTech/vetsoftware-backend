package com.vetsoftware.app.companybillingprofile.application.port.in;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** El historico de fichas de una empresa, de la vigente a la mas antigua. */
public interface ListCompanyBillingProfilesUseCase {

    /**
     * <strong>Se llama {@code listByCompany} y no {@code listAll}, y no es una
     * preferencia de nombre.</strong> Un listado que no filtra por empresa solo lo
     * puede servir {@code hasRole('SYSTEM')} a secas
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, dura), y aqui devolver filas de
     * todos los tenants seria publicar el NIT, la direccion y el correo de
     * facturacion de cada clinica de la plataforma. No existe la hermana ancha ni
     * en este puerto ni en el de salida.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<CompanyBillingProfileDto> listByCompany(Long companyId, int page, int pageSize);
}
