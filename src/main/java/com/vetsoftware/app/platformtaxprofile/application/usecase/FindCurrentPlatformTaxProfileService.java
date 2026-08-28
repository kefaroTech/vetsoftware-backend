package com.vetsoftware.app.platformtaxprofile.application.usecase;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.in.FindCurrentPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.NoCurrentPlatformTaxProfileException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La identidad fiscal que rige hoy.
 *
 * <p>
 * <strong>Falla en voz alta en vez de devolver vacio, y es la decision del
 * bloque.</strong> Mientras nadie siembre la primera fila —que hoy es el estado
 * real, porque el changeset 367 dejo la tabla vacia a proposito— esto contesta
 * {@link NoCurrentPlatformTaxProfileException}. Es la misma eleccion que
 * {@code platform_billing_config} (255) hace con
 * {@code PlatformBillingConfigNotConfiguredException}: preferible que la
 * operacion muera ruidosamente a que se emita una factura con la razon social
 * en blanco o inventada.
 */
@Observed(name = "platform.tax.profile.find.current")
@Service
public class FindCurrentPlatformTaxProfileService implements FindCurrentPlatformTaxProfileUseCase {

    private final PlatformTaxProfileRepository repository;

    public FindCurrentPlatformTaxProfileService(PlatformTaxProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public PlatformTaxProfileDto findCurrent() {
        return repository.findCurrent().map(PlatformTaxProfileDto::from)
                .orElseThrow(NoCurrentPlatformTaxProfileException::new);
    }
}
