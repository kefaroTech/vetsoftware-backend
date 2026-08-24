package com.vetsoftware.app.platformbillingconfig.application.usecase;

import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import com.vetsoftware.app.platformbillingconfig.application.port.in.FindPlatformBillingConfigUseCase;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PlatformBillingConfigRepository;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfigNotConfiguredException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sirve las políticas de facturación de la plataforma.
 *
 * <p>
 * El {@code orElseThrow} de aquí es la regla entera de esta feature: la
 * ausencia de la fila <b>no</b> se degrada a un valor por defecto ni se propaga
 * como {@code Optional} vacío. Si se degradara, el sistema seguiría corriendo
 * con unos días de gracia que nadie decidió y el defecto solo se vería semanas
 * después, en una cuenta cortada antes de tiempo.
 */
@Observed(name = "platform.billing.config.find")
@Service
public class FindPlatformBillingConfigService implements FindPlatformBillingConfigUseCase {
    private final PlatformBillingConfigRepository repository;

    public FindPlatformBillingConfigService(PlatformBillingConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformBillingConfigDto find() {
        return PlatformBillingConfigDto.from(
                repository.find().orElseThrow(PlatformBillingConfigNotConfiguredException::new));
    }
}
