package com.vetsoftware.app.securityincident.application.usecase;

import com.vetsoftware.app.securityincident.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import com.vetsoftware.app.securityincident.application.port.in.RegisterAffectedCompanyUseCase;
import com.vetsoftware.app.securityincident.application.port.out.CompanyValidationPort;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentCompanyRepository;
import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.AffectedCompanyAlreadyRegisteredException;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota que una clinica quedo alcanzada por un incidente.
 *
 * <p>
 * Tres comprobaciones, y las tres son hechos externos que el dominio no puede
 * conocer: que el incidente existe, que la clinica existe y que esa terna no
 * estaba ya anotada. Las invariantes de la fila —contador no negativo, ambito
 * obligatorio— viven en {@link SecurityIncidentCompany}.
 *
 * <p>
 * <strong>La comprobacion de duplicado no sustituye a
 * {@code uq_sic_pair}</strong>, que es quien de verdad lo impide: dos
 * peticiones concurrentes pasarian las dos por este {@code exists}. Esta aqui
 * para que el caso normal responda «ya estaba registrada» con su nombre en vez
 * de una violacion de integridad cruda; la carrera la sigue perdiendo la base,
 * que es lo correcto.
 */
@Observed(name = "security.incident.affected.register")
@Service
public class RegisterAffectedCompanyService implements RegisterAffectedCompanyUseCase {

    private final SecurityIncidentCompanyRepository affectedRepository;
    private final SecurityIncidentRepository incidentRepository;
    private final CompanyValidationPort companyValidationPort;

    public RegisterAffectedCompanyService(SecurityIncidentCompanyRepository affectedRepository,
            SecurityIncidentRepository incidentRepository,
            CompanyValidationPort companyValidationPort) {
        this.affectedRepository = affectedRepository;
        this.incidentRepository = incidentRepository;
        this.companyValidationPort = companyValidationPort;
    }

    @Override
    @Transactional
    public AffectedCompanyDto execute(RegisterAffectedCompanyCommand command) {
        if (incidentRepository.findById(command.securityIncidentId()).isEmpty())
            throw new SecurityIncidentNotFoundException(command.securityIncidentId());
        if (!companyValidationPort.existsById(command.companyId()))
            throw new IllegalArgumentException("Company not found: " + command.companyId());
        if (affectedRepository.existsByIncidentIdAndCompanyIdAndScope(command.securityIncidentId(),
                command.companyId(), command.affectedScope()))
            throw new AffectedCompanyAlreadyRegisteredException(command.securityIncidentId(),
                    command.companyId(), command.affectedScope());
        SecurityIncidentCompany affected = SecurityIncidentCompany.register(
                command.securityIncidentId(), command.companyId(), command.affectedScope(),
                command.affectedSubjectCount());
        return AffectedCompanyDto.from(affectedRepository.save(affected));
    }
}
