package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSecurityIncidentsUseCase {

    /**
     * El barrido de plataforma: todos los incidentes, lo mas reciente primero.
     *
     * <p>
     * <strong>No filtra por empresa porque la tabla no tiene empresa</strong>, y
     * por eso {@code hasRole('SYSTEM')} a secas es el unico gate posible
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, BE-29). Aqui no existe el hermano
     * acotado que otras rodajas ofrecen al tenant: lo que una clinica podria ver de
     * un incidente —que la alcanzo— es una consulta sobre la puente y no sobre esta
     * tabla, y sale de un caso de uso que todavia no existe. Queda dicho.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SecurityIncidentDto> listAll(int page, int pageSize);
}
