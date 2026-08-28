package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import com.vetsoftware.app.securityincident.application.port.in.FindSecurityIncidentUseCase;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "security.incident.find")
@Service
public class FindSecurityIncidentService implements FindSecurityIncidentUseCase {

    private final SecurityIncidentRepository repository;

    public FindSecurityIncidentService(SecurityIncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityIncidentDto findById(Long id) {
        return repository.findById(id).map(SecurityIncidentDto::from)
                .orElseThrow(() -> new SecurityIncidentNotFoundException(id));
    }
}
