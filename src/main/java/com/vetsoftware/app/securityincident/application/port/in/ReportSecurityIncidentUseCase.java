package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.command.ReportSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReportSecurityIncidentUseCase {

    /**
     * Anota el reporte a la autoridad con su radicado. Solo plataforma: es
     * VetSoftware quien reporta, no la clinica.
     *
     * <p>
     * {@code hasRole('SYSTEM')} a secas satisface tambien a
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}: el command señala una
     * fila por su id y no transporta empresa, y es correcto porque la tabla no la
     * tiene.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SecurityIncidentDto execute(ReportSecurityIncidentCommand command);
}
