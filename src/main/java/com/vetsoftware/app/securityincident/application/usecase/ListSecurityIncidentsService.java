package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.in.ListSecurityIncidentsUseCase;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El barrido de plataforma. No filtra por empresa porque la tabla no la tiene:
 * su puerto de entrada esta cerrado a {@code ROLE_SYSTEM} a secas, que es lo
 * unico que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} admite.
 */
@Observed(name = "security.incident.list")
@Service
public class ListSecurityIncidentsService implements ListSecurityIncidentsUseCase {

    private final SecurityIncidentRepository repository;

    public ListSecurityIncidentsService(SecurityIncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SecurityIncidentDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(SecurityIncidentDto::from);
    }
}
