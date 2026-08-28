package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.command.CloseSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.in.CloseSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra el incidente con su contencion y su causa raiz.
 *
 * <p>
 * Igual que en el reporte, {@code closedAt} viene del command: el cierre
 * documenta cuando se contuvo de verdad, que puede ser dias antes de que
 * alguien lo escriba. Que sin contencion ni causa raiz no se cierre —espejo de
 * {@code chk_security_incidents_close}— lo decide el dominio.
 */
@Observed(name = "security.incident.close")
@Service
public class CloseSecurityIncidentService implements CloseSecurityIncidentUseCase {

    private final SecurityIncidentRepository repository;

    public CloseSecurityIncidentService(SecurityIncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SecurityIncidentDto execute(CloseSecurityIncidentCommand command) {
        SecurityIncident incident = repository.findById(command.id())
                .orElseThrow(() -> new SecurityIncidentNotFoundException(command.id()));
        SecurityIncident closed = incident.close(command.closedAt(), command.containment(),
                command.rootCause(), command.notifiedSubjectsAt());
        return SecurityIncidentDto.from(repository.save(closed));
    }
}
