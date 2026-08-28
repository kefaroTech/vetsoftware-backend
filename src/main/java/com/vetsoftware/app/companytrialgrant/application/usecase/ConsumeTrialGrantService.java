package com.vetsoftware.app.companytrialgrant.application.usecase;

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ConsumeTrialGrantUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrantNotFoundException;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve una prueba escribiendo su desenlace.
 *
 * <p>
 * Cuando el desenlace no viene dado, se deriva de la política congelada en la
 * concesión —no de la política que el catálogo tenga hoy—: si mañana se cambia
 * Historia clínica de {@code LIMITED} a {@code READ_ONLY}, a quien ya estaba
 * probando no le cambia nada.
 */
@Service
public class ConsumeTrialGrantService implements ConsumeTrialGrantUseCase {

    private final CompanyTrialGrantRepository repository;
    private final Clock clock;

    public ConsumeTrialGrantService(CompanyTrialGrantRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyTrialGrantDto execute(ConsumeTrialGrantCommand command) {
        CompanyTrialGrant grant = repository
                .findByCompanyIdAndCatalogItemId(command.companyId(), command.catalogItemId())
                .orElseThrow(() -> new CompanyTrialGrantNotFoundException(command.companyId(),
                        command.catalogItemId()));
        TrialOutcome outcome = command.outcome() == null
                ? grant.getPolicyTrialOutcome().resolvedOutcome()
                : command.outcome();
        return CompanyTrialGrantDto
                .from(repository.save(grant.consume(LocalDateTime.now(clock), outcome)));
    }
}
