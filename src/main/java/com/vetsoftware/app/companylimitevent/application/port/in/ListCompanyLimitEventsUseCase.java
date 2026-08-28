package com.vetsoftware.app.companylimitevent.application.port.in;

import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Los hechos de cupo de una empresa en un rango.
 *
 * <p>
 * Con esto, «¿cuántas veces topó el techo en marzo?» es una consulta — y una
 * oportunidad comercial que hoy se pierde entera.
 */
public interface ListCompanyLimitEventsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyLimitEvent.read')"
            + " and @authz.isMyCompany(#companyId))")
    List<CompanyLimitEventDto> listByCompanyId(Long companyId, LocalDateTime from,
            LocalDateTime to);
}
