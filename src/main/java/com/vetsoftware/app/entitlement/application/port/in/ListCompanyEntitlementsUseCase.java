package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El listado completo de permisos de una empresa, incluidos los {@code NONE} y
 * los caducados: es la vista de auditoria --"por que esta clinica no ve
 * facturacion"-- y no la que pinta el menu.
 *
 * <p>
 * Filtra siempre por empresa. No existe, ni debe existir, la variante sin
 * filtro: seria devolver filas de todos los tenants.
 */
public interface ListCompanyEntitlementsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('entitlement.read') "
            + "and @authz.isMyCompany(#companyId))")
    PageResult<CompanyEntitlementDto> listByCompanyId(Long companyId, int page, int pageSize);
}
