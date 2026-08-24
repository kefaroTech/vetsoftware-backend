package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.port.in.RecalculateCompanyEntitlementsUseCase;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El caso de uso central del slice: reconstruir lo que una empresa puede usar a
 * partir de lo unico que es verdad, su contrato.
 *
 * <p>
 * Es la via <strong>autorizada</strong>: la usa la plataforma y la usa el
 * tenant para reparar su propia empresa cuando {@code recalculatedAt} se ha
 * quedado viejo. El alta de una empresa nueva no puede pasar por aqui --no hay
 * principal-- y tiene su propio caso de uso interno,
 * {@code InitializeCompanyEntitlementsUseCase}.
 *
 * <p>
 * Toda la mecanica vive en {@link CompanyEntitlementRecalculator} y toda la
 * regla en {@code EntitlementCalculator}; este servicio pone el gate y la
 * transaccion.
 */
@Observed(name = "entitlement.recalculate")
@Service
public class RecalculateCompanyEntitlementsService
        implements
            RecalculateCompanyEntitlementsUseCase {

    private final CompanyEntitlementRecalculator recalculator;

    RecalculateCompanyEntitlementsService(CompanyEntitlementRecalculator recalculator) {
        this.recalculator = recalculator;
    }

    @Override
    @Transactional
    public EntitlementRecalculationDto execute(RecalculateCompanyEntitlementsCommand command) {
        return recalculator.recalculate(command.companyId());
    }
}
