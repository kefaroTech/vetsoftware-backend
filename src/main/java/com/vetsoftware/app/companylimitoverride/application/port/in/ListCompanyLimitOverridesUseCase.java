package com.vetsoftware.app.companylimitoverride.application.port.in;

import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La historia de excepciones de una empresa, revocadas incluidas. Es lo que
 * responde «¿qué techo tenía el 14 de marzo?» sin reconstruir nada.
 */
public interface ListCompanyLimitOverridesUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyLimitOverride.read')"
            + " and @authz.isMyCompany(#companyId))")
    List<CompanyLimitOverrideDto> listByCompanyId(Long companyId);
}
