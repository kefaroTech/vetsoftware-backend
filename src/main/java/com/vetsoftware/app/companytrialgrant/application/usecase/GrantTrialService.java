package com.vetsoftware.app.companytrialgrant.application.usecase;

import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.GrantTrialUseCase;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import com.vetsoftware.app.companytrialgrant.application.port.out.TrialWindowQueryPort;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.TrialAlreadyGrantedException;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowNotOpenException;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowRef;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concede la prueba de un artículo.
 *
 * <p>
 * <strong>Reponer un módulo no crea una concesión nueva.</strong> Si ya hay
 * una, esto falla: la unicidad {@code (company_id, catalog_item_id)} lo impone
 * en el motor y aquí se comprueba antes solo para que el mensaje diga qué pasó.
 * Quien repone un módulo quitado lee la concesión existente —con los días que
 * le quedaban— en vez de pedir otra. Si esta comprobación se «arreglara»
 * dejándola pasar, quitar un módulo el día 29 y reponerlo el 30 sería software
 * gratis indefinido y ninguna fila del modelo estaría mal.
 *
 * <p>
 * La ventana se resuelve por la empresa y no por un id de fuera: eso es lo que
 * impide que la prueba de una clínica cuelgue de la ventana de otra.
 */
@Service
public class GrantTrialService implements GrantTrialUseCase {

    private final CompanyTrialGrantRepository repository;
    private final TrialWindowQueryPort trialWindowQueryPort;
    private final Clock clock;

    public GrantTrialService(CompanyTrialGrantRepository repository,
            TrialWindowQueryPort trialWindowQueryPort, Clock clock) {
        this.repository = repository;
        this.trialWindowQueryPort = trialWindowQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyTrialGrantDto execute(GrantTrialCommand command) {
        if (repository.existsByCompanyIdAndCatalogItemId(command.companyId(),
                command.catalogItemId()))
            throw new TrialAlreadyGrantedException(command.companyId(), command.catalogItemId());
        TrialWindowRef window = trialWindowQueryPort.findOpenByCompanyId(command.companyId())
                .orElseThrow(() -> new TrialWindowNotOpenException(command.companyId(),
                        command.grantedOn()));
        CompanyTrialGrant grant = CompanyTrialGrant.grant(window, command.catalogItemId(),
                command.grantedOn(), command.daysGranted(), command.policyTrialDays(),
                command.policyTrialOutcome(), command.sourceQuoteId(),
                command.grantingAmendmentId(), LocalDateTime.now(clock));
        return CompanyTrialGrantDto.from(repository.save(grant));
    }
}
