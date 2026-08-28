package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindSecurityIncidentUseCase {

    /** El expediente completo de un incidente. Solo plataforma. */
    @PreAuthorize("hasRole('SYSTEM')")
    SecurityIncidentDto findById(Long id);
}
