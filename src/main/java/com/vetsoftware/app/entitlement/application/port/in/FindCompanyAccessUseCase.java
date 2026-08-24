package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La consulta caliente: que puede usar esta empresa ahora mismo.
 *
 * <p>
 * No exige un permiso concreto y es deliberado: <strong>todo empleado necesita
 * saber que modulos tiene su clinica</strong> para que la aplicacion pueda
 * pintarse. Lo que si exige es que la empresa sea la suya --o un principal
 * SYSTEM--, que es lo unico que hay que proteger aqui.
 *
 * <p>
 * Se resuelve con un unico rango sobre {@code uq_company_entitlements}: por
 * empresa hay del orden de 15-40 filas y se traen todas de una vez. Ni barrido
 * de tabla ni N+1 al pintar el menu.
 */
public interface FindCompanyAccessUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#companyId)")
    CompanyAccessDto findByCompanyId(Long companyId);
}
