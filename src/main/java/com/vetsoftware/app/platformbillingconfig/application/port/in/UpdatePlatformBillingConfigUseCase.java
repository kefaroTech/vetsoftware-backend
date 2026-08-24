package com.vetsoftware.app.platformbillingconfig.application.port.in;

import com.vetsoftware.app.platformbillingconfig.application.command.UpdatePlatformBillingConfigCommand;
import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambia las políticas de facturación de la plataforma. Es la mitad de
 * escritura del par lectura + actualización: <b>no crea la fila</b>. Si no
 * existe lanza {@code PlatformBillingConfigNotConfiguredException}, igual que
 * la lectura, en vez de hacer un upsert que inventaría políticas que nadie
 * decidió.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. La configuración es global
 * de plataforma, así que el command no lleva —ni puede llevar— un
 * {@code companyId} que revalidar con {@code @authz.isMyCompany(...)}.
 */
public interface UpdatePlatformBillingConfigUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PlatformBillingConfigDto execute(UpdatePlatformBillingConfigCommand command);
}
