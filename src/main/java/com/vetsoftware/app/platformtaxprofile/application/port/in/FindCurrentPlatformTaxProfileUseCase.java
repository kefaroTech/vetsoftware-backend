package com.vetsoftware.app.platformtaxprofile.application.port.in;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La identidad fiscal que rige hoy: la unica fila de la tabla con
 * {@code valid_to} nulo.
 *
 * <p>
 * <strong>Es el puerto que va a consumir la emision de facturas</strong> cuando
 * se cablee el {@code platform_tax_profile_id} que el changeset 368 añadio a
 * {@code subscription_billing_documents} —resolver la vigente al emitir y
 * congelarla en la columna, mismo patron que
 * {@code JpaCompanyFiscalProfileQueryPort.toIssuer()}—. Eso todavia no esta
 * hecho.
 */
public interface FindCurrentPlatformTaxProfileUseCase {

    /**
     * @throws com.vetsoftware.app.platformtaxprofile.domain.NoCurrentPlatformTaxProfileException
     *             si no hay ninguna vigente, que hoy es el estado normal: la tabla
     *             nace sin sembrar a proposito
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PlatformTaxProfileDto findCurrent();
}
