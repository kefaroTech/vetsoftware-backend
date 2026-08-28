package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.command.CloseSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CloseSecurityIncidentUseCase {

    /**
     * Cierra el incidente con su contencion y su causa raiz escritas.
     *
     * <p>
     * No hay borrado en toda la rodaja, y este es el motivo: un incidente se
     * <em>cierra</em>, que es escribir como acabo. Retirarlo dejaria sin
     * explicacion el reporte ya presentado a la autoridad.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SecurityIncidentDto execute(CloseSecurityIncidentCommand command);
}
