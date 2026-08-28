package com.vetsoftware.app.platformtaxprofile.application.usecase;

import com.vetsoftware.app.platformtaxprofile.application.command.OpenPlatformTaxProfileCommand;
import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import com.vetsoftware.app.platformtaxprofile.application.port.in.OpenPlatformTaxProfileUseCase;
import com.vetsoftware.app.platformtaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.platformtaxprofile.application.port.out.PlatformTaxProfileRepository;
import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfileAlreadyOpenException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la <strong>primera</strong> identidad fiscal de VetSoftware.
 *
 * <p>
 * <strong>Este es el caso de uso que la tabla lleva esperando desde el
 * changeset 367.</strong> Aquel dejo {@code platform_tax_profiles} vacia a
 * proposito —no habia razon social ni NIT reales y no se inventaron, porque una
 * identidad fiscal inventada acaba impresa en la factura de cada cliente—, y la
 * siembra es una decision del dueño, no un despliegue. Este servicio es la via
 * por la que entra cuando la tomen.
 *
 * <p>
 * <strong>La comprobacion previa no sustituye a
 * {@code uq_platform_tax_profiles_current}: la traduce.</strong> Entre la
 * lectura y el {@code INSERT} cabe otra transaccion, asi que lo unico que
 * garantiza que no haya dos identidades vigentes es la columna generada y su
 * indice unico. Lo que se gana aqui es que el caso comun —el boton pulsado dos
 * veces, o quien busca «crear» cuando lo que toca es «suceder»— conteste un 409
 * que explica que existe la sucesion, en vez de un 500 con un
 * {@code Duplicate entry} sobre una columna que no aparece en ningun sitio del
 * codigo.
 *
 * <p>
 * <strong>La actividad economica se resuelve aqui y no en el
 * repositorio.</strong> Es lo que pide el CLAUDE.md para las FK cross-feature:
 * el adaptador usa {@code getReferenceById} sin validar, y quien traduce «ese
 * id no existe» en un mensaje con el id delante es este servicio. Es opcional
 * —{@code economic_activity_id} es nulable en 367—, asi que un {@code null}
 * pasa sin consultar nada.
 */
@Observed(name = "platform.tax.profile.open")
@Service
public class OpenPlatformTaxProfileService implements OpenPlatformTaxProfileUseCase {

    private final PlatformTaxProfileRepository repository;
    private final EconomicActivityQueryPort economicActivityQueryPort;
    private final Clock clock;

    public OpenPlatformTaxProfileService(PlatformTaxProfileRepository repository,
            EconomicActivityQueryPort economicActivityQueryPort, Clock clock) {
        this.repository = repository;
        this.economicActivityQueryPort = economicActivityQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PlatformTaxProfileDto execute(OpenPlatformTaxProfileCommand command) {
        repository.findCurrent().ifPresent(current -> {
            throw new PlatformTaxProfileAlreadyOpenException(current.getId(),
                    current.getValidFrom());
        });

        EconomicActivityRef economicActivity = resolveEconomicActivity(
                command.economicActivityId());

        PlatformTaxProfile profile = PlatformTaxProfile.open(command.documentType(),
                command.documentId(), command.verificationDigit(), command.legalName(),
                command.taxRegime(), command.fiscalEmail(), command.commercialName(),
                economicActivity, command.selfWithholder(), command.validFrom(),
                LocalDateTime.now(clock));

        return PlatformTaxProfileDto.from(repository.save(profile));
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
