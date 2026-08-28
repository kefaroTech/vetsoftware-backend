package com.vetsoftware.app.platformtaxprofile.application.usecase;

import com.vetsoftware.app.platformtaxprofile.application.command.SucceedPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.in.SucceedPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.platformtaxprofile.domain.NoCurrentPlatformTaxProfileException;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El cambio de identidad fiscal de VetSoftware: cierra la vigente y abre su
 * sucesora, <strong>en una sola transaccion</strong>.
 *
 * <h2>Por que no es un update</h2>
 *
 * <p>
 * Reescribir la fila cambiaria hacia atras con que razon social y que NIT se
 * emitieron las facturas anteriores. El changeset 368 añadio
 * {@code platform_tax_profile_id} a {@code subscription_billing_documents}
 * justo para que cada documento apunte a la identidad con la que se emitio; si
 * la fila se editara en sitio, la factura del año pasado seguiria enlazada a la
 * misma fila y esa fila diria otra cosa. Cerrar y abrir deja las dos verdades:
 * la de entonces y la de ahora.
 *
 * <h2>El orden de las dos escrituras importa, y no por lo que parece</h2>
 *
 * <p>
 * Se cierra primero y se inserta despues, pero escribirlo en ese orden
 * <strong>no basta</strong>: Hibernate ejecuta todos los {@code INSERT} antes
 * que los {@code UPDATE} de la misma transaccion, asi que la sucesora entraria
 * mientras la anterior sigue vigente, las dos calcularian el mismo
 * {@code current_profile_marker} —que aqui es la constante {@code 1}, no
 * {@code company_id}— y {@code uq_platform_tax_profiles_current} pararia la
 * operacion. Lo que lo resuelve es que
 * {@code PlatformTaxProfileRepository.save} vacia el buffer antes de devolver,
 * y por eso ese flush esta escrito en el contrato del puerto y no escondido en
 * el adaptador.
 *
 * <h2>Sucesion en el mismo dia</h2>
 *
 * <p>
 * No es representable: {@code chk_platform_tax_profiles_validity} es
 * {@code valid_to > valid_from} estricto y
 * {@code uq_platform_tax_profiles_validity} no admite dos identidades empezando
 * el mismo dia. El dominio la rechaza con
 * {@code PlatformTaxProfileSuccessionNotAfterCurrentException}, que dice desde
 * cuando rige la vigente y cual es la primera fecha posible. <strong>No se
 * adelanta al dia siguiente por cuenta propia</strong>: esa fecha es la que
 * decide que razon social se imprime en una factura emitida en el intervalo.
 */
@Observed(name = "platform.tax.profile.succeed")
@Service
public class SucceedPlatformTaxProfileService implements SucceedPlatformTaxProfileUseCase {

    private final PlatformTaxProfileRepository repository;
    private final EconomicActivityQueryPort economicActivityQueryPort;
    private final Clock clock;

    public SucceedPlatformTaxProfileService(PlatformTaxProfileRepository repository,
            EconomicActivityQueryPort economicActivityQueryPort, Clock clock) {
        this.repository = repository;
        this.economicActivityQueryPort = economicActivityQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PlatformTaxProfileDto execute(SucceedPlatformTaxProfileCommand command) {
        PlatformTaxProfile current = repository.findCurrent()
                .orElseThrow(NoCurrentPlatformTaxProfileException::new);

        // La actividad economica se resuelve ANTES de cerrar nada: un id que no
        // existe tiene que dejar la identidad vigente intacta. Con la transaccion
        // basta para que el UPDATE no cuaje, pero resolver antes hace que el orden
        // de las escrituras no dependa del rollback.
        EconomicActivityRef economicActivity = resolveEconomicActivity(
                command.economicActivityId());

        current.closeOn(command.effectiveFrom());
        repository.save(current);

        PlatformTaxProfile successor = PlatformTaxProfile.open(command.documentType(),
                command.documentId(), command.verificationDigit(), command.legalName(),
                command.taxRegime(), command.fiscalEmail(), command.commercialName(),
                economicActivity, command.selfWithholder(), command.effectiveFrom(),
                LocalDateTime.now(clock));

        return PlatformTaxProfileDto.from(repository.save(successor));
    }

    /**
     * {@code null} entra y {@code null} sale: la actividad economica es opcional
     * porque la columna es nulable. Un id que no existe —o que existe pero esta
     * dado de baja, porque {@code EconomicActivityJpaEntity} lleva
     * {@code @SQLRestriction("enabled = true")}— sale como un 400 con el id
     * delante.
     */
    private EconomicActivityRef resolveEconomicActivity(Long economicActivityId) {
        if (economicActivityId == null)
            return null;
        return economicActivityQueryPort.findById(economicActivityId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Economic activity not found: " + economicActivityId));
    }
}
