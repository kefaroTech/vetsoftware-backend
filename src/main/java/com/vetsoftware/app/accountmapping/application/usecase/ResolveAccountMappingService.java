package com.vetsoftware.app.accountmapping.application.usecase;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.in.ResolveAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import com.vetsoftware.app.accountmapping.domain.NoEffectiveAccountMappingException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * El mapeo vigente para un supuesto en una fecha: <strong>la consulta por la
 * que existe toda esta feature</strong>.
 *
 * <p>
 * <strong>Lanza en vez de devolver vacio, y ese {@code orElseThrow} es la unica
 * linea interesante de la clase.</strong> Si el supuesto no tiene mapeo —porque
 * nadie lo sembro, o porque el vigente se cerro sin abrir su relevo— un
 * {@code Optional} vacio se lee como «no habia nada que asentar» y el asiento
 * simplemente no sale. Nadie se entera hasta que el balance de prueba no
 * cuadra, meses despues y sin rastro de por donde entro.
 *
 * <p>
 * <strong>El dia por defecto lo pone este metodo con el {@code Clock}
 * inyectado, no el controller.</strong> Un {@code LocalDate.now()} en la capa
 * web seria una fecha que ningun test puede fijar y
 * {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} rompe el build por ello.
 */
@Observed(name = "account.mapping.resolve")
@Service
public class ResolveAccountMappingService implements ResolveAccountMappingUseCase {

    private final AccountMappingRepository repository;
    private final Clock clock;

    public ResolveAccountMappingService(AccountMappingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public AccountMappingDto resolve(MappingKind mappingKind, String mappingKey, Long catalogItemId,
            String chargeType, String taxTreatment, LocalDate on) {
        LocalDate effectiveOn = on == null ? LocalDate.now(clock) : on;
        return repository
                .findEffective(mappingKind, mappingKey, catalogItemId, chargeType, taxTreatment,
                        effectiveOn)
                .map(AccountMappingDto::from)
                .orElseThrow(() -> new NoEffectiveAccountMappingException(mappingKind, mappingKey,
                        catalogItemId, chargeType, taxTreatment, effectiveOn));
    }
}
