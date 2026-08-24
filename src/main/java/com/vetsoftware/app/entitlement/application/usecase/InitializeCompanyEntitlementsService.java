package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La primera derivacion de permisos de una empresa, dentro de la transaccion
 * que la esta dando de alta.
 *
 * <p>
 * La propagacion es la de por defecto ({@code REQUIRED}) <strong>a
 * proposito</strong>: se une a la transaccion del alta en vez de abrir una
 * propia, que es lo que exige R10 --si el recalculo falla, la empresa no
 * nace--. Con {@code REQUIRES_NEW} quedaria una empresa creada y sin permisos,
 * que es exactamente el estado que este caso de uso existe para impedir.
 *
 * <p>
 * Comparte mecanica con {@code RecalculateCompanyEntitlementsService} a traves
 * de {@link CompanyEntitlementRecalculator}: lo unico distinto entre los dos es
 * quien puede llamarlos.
 */
@Observed(name = "entitlement.initialize")
@Service
public class InitializeCompanyEntitlementsService implements InitializeCompanyEntitlementsUseCase {

    private final CompanyEntitlementRecalculator recalculator;

    InitializeCompanyEntitlementsService(CompanyEntitlementRecalculator recalculator) {
        this.recalculator = recalculator;
    }

    @Override
    @Transactional
    public EntitlementRecalculationDto execute(InitializeCompanyEntitlementsCommand command) {
        return recalculator.recalculate(command.companyId());
    }
}
