package com.vetsoftware.app.companytrialwindow.application.port.in;

import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La ventana viva de una empresa: hasta cuándo dura su prueba.
 *
 * <p>
 * Autorización: plataforma, o el propio tenant sobre su empresa. Recibe el
 * {@code companyId} como parámetro y lo revalida contra el principal, que es lo
 * que impide leer el reloj de la clínica vecina escribiendo otro número.
 */
public interface FindCurrentTrialWindowUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyTrialWindow.read')"
            + " and @authz.isMyCompany(#companyId))")
    CompanyTrialWindowDto findOpenByCompanyId(Long companyId);
}
