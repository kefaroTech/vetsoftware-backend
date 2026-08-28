package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.securityincident.application.port.in.ListAffectedCompaniesUseCase;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentCompanyRepository;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las clinicas alcanzadas por un incidente.
 *
 * <p>
 * Comprueba antes que el incidente existe para responder 404 y no una pagina
 * vacia: «este incidente no alcanzo a nadie» y «este incidente no existe» son
 * respuestas distintas, y confundirlas hace perder el tiempo a quien audita.
 */
@Observed(name = "security.incident.affected.list")
@Service
public class ListAffectedCompaniesService implements ListAffectedCompaniesUseCase {

    private final SecurityIncidentCompanyRepository affectedRepository;
    private final SecurityIncidentRepository incidentRepository;

    public ListAffectedCompaniesService(SecurityIncidentCompanyRepository affectedRepository,
            SecurityIncidentRepository incidentRepository) {
        this.affectedRepository = affectedRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AffectedCompanyDto> listByIncident(Long securityIncidentId, int page,
            int pageSize) {
        if (incidentRepository.findById(securityIncidentId).isEmpty())
            throw new SecurityIncidentNotFoundException(securityIncidentId);
        return affectedRepository.findByIncidentId(securityIncidentId, page, pageSize)
                .map(AffectedCompanyDto::from);
    }
}
