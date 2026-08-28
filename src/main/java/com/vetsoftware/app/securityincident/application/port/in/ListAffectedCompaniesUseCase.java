package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAffectedCompaniesUseCase {

    /**
     * Las clinicas alcanzadas por un incidente.
     *
     * <p>
     * <strong>Este listado no puede abrirse a un tenant por ningun
     * permiso.</strong> Devuelve filas de todas las clinicas alcanzadas —cual, por
     * que ambito y cuantos titulares de cada una—, que es informacion de las demas.
     * Es exactamente el caso que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} describe,
     * y aqui la regla y el negocio dicen lo mismo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<AffectedCompanyDto> listByIncident(Long securityIncidentId, int page, int pageSize);
}
