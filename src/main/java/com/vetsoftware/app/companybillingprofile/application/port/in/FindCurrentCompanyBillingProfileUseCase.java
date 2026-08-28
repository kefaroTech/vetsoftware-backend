package com.vetsoftware.app.companybillingprofile.application.port.in;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** La ficha que rige hoy para una empresa. */
public interface FindCurrentCompanyBillingProfileUseCase {

    /**
     * <p>
     * <strong>Aqui el {@code #companyId} si es el nombre del parametro</strong>, al
     * reves que en los dos puertos de escritura, donde es
     * {@code #command.companyId}. Es la diferencia que produce el fallo silencioso
     * que advierte el CLAUDE.md, asi que se revisa cada vez que se copia una de
     * estas anotaciones.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.read') and"
            + " @authz.isMyCompany(#companyId))")
    CompanyBillingProfileDto findCurrent(Long companyId);
}
