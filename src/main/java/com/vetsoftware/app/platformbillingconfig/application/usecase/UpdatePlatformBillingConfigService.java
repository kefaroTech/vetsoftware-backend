package com.vetsoftware.app.platformbillingconfig.application.usecase;

import com.vetsoftware.app.platformbillingconfig.application.command.UpdatePlatformBillingConfigCommand;
import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import com.vetsoftware.app.platformbillingconfig.application.port.in.UpdatePlatformBillingConfigUseCase;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PlatformBillingConfigRepository;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfigNotConfiguredException;
import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia las políticas de facturación de la plataforma.
 *
 * <p>
 * Carga la fila existente y la actualiza: <b>no la crea</b>. Si no está,
 * termina en la misma excepción que la lectura, con el mismo mensaje y el mismo
 * remedio. Un upsert aquí sería peor que un fallo: haría que un despliegue sin
 * semilla se "arreglara" solo con lo que hubiera tecleado el primer
 * administrador que abriera el formulario.
 *
 * <p>
 * Las invariantes (rangos de días, el 1–28 del día de emisión) las valida el
 * dominio en {@link PlatformBillingConfig#update}, no este servicio.
 */
@Observed(name = "platform.billing.config.update")
@Service
public class UpdatePlatformBillingConfigService implements UpdatePlatformBillingConfigUseCase {
    private final PlatformBillingConfigRepository repository;
    private final PriceListQueryPort priceListQueryPort;

    public UpdatePlatformBillingConfigService(PlatformBillingConfigRepository repository,
            PriceListQueryPort priceListQueryPort) {
        this.repository = repository;
        this.priceListQueryPort = priceListQueryPort;
    }

    @Override
    @Transactional
    public PlatformBillingConfigDto execute(UpdatePlatformBillingConfigCommand command) {
        PlatformBillingConfig config = repository.find()
                .orElseThrow(PlatformBillingConfigNotConfiguredException::new);
        config.update(resolvePriceList(command.defaultPriceListId()), command.defaultGraceDays(),
                command.defaultTrialDays(), command.invoiceDayOfMonth(),
                command.defaultPaymentTermDays(), command.externalBillingProvider());
        return PlatformBillingConfigDto.from(repository.save(config));
    }

    /**
     * {@code null} es una respuesta legítima: la columna es nulable y quitar la
     * tarifa por defecto es una decisión válida. Lo que no es legítimo es apuntar a
     * una lista que no existe.
     */
    private PriceListRef resolvePriceList(Long priceListId) {
        if (priceListId == null)
            return null;
        return priceListQueryPort.findById(priceListId).orElseThrow(
                () -> new IllegalArgumentException("Price list not found: " + priceListId));
    }
}
