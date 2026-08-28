package com.vetsoftware.app.publicholiday.application.port.in;

import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Leen los dos lados, y por eso el puerto <strong>recibe la empresa aunque la
 * tabla no la tenga</strong>: es lo que permite exigirle al empleado que
 * declare la suya y reservar la via ancha a {@code ROLE_SYSTEM}, en vez de
 * abrir por autoridad suelta lo que el resto del producto cierra por tenant.
 */
public interface FindPublicHolidayUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('holiday.read') and @authz.isMyCompany(#companyId))")
    PublicHolidayDto findById(Long id, Long companyId);
}
