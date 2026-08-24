package com.vetsoftware.app.dunning.application.port.in;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consulta cross-tenant de cobranza para la consola de plataforma. */
public interface ListAllDunningEventsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<DunningEventDto> listAll(Long companyId, int page, int pageSize);
}
