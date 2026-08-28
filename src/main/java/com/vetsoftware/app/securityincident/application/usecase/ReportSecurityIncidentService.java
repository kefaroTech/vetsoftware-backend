package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.command.ReportSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.in.ReportSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota el reporte a la autoridad con su radicado.
 *
 * <p>
 * <strong>La fecha viene del command y no del reloj</strong>, y es la unica
 * decision de este servicio. El reporte se presenta por el micrositio de la
 * Delegatura, fuera de este sistema; sellar aqui el instante convertiria «se
 * reporto el dia 12» en «se registro el dia 19» y borraria la prueba de que se
 * cumplio el plazo. El reloj inyectado no hace falta porque no hay nada que
 * fechar.
 *
 * <p>
 * Que no se pueda reportar dos veces lo decide {@link SecurityIncident#report},
 * que es donde vive la invariante. Comprobarlo tambien aqui invitaria a que un
 * dia solo quedara la copia de este lado.
 */
@Observed(name = "security.incident.report")
@Service
public class ReportSecurityIncidentService implements ReportSecurityIncidentUseCase {

    private final SecurityIncidentRepository repository;

    public ReportSecurityIncidentService(SecurityIncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SecurityIncidentDto execute(ReportSecurityIncidentCommand command) {
        SecurityIncident incident = repository.findById(command.id())
                .orElseThrow(() -> new SecurityIncidentNotFoundException(command.id()));
        SecurityIncident reported = incident.report(command.reportedAt(),
                command.reportReference());
        return SecurityIncidentDto.from(repository.save(reported));
    }
}
