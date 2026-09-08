package com.vetsoftware.app.animal.infrastructure.orchestration;

import com.vetsoftware.app.animal.application.port.out.AnimalUsageLimitPort;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * ANIMAL es {@code CUMULATIVE}: cuenta desde siempre, sin reinicio mensual.
 *
 * <p>
 * {@code RecordCompanyUsageEventUseCase} es {@code hasRole('SYSTEM')} a secas
 * (es la prueba de un cobro y no la escribe la parte a la que se le cobra), así
 * que anotar el hecho corre bajo {@link SystemAuthRunner} aunque quien está
 * dando de alta la mascota sea un empleado corriente.
 */
@Component
public class EntitlementAnimalUsageLimitAdapter implements AnimalUsageLimitPort {

    private static final String DIMENSION_CODE = "ANIMAL";
    // Espejo del centinela UsagePeriodKey.ALLTIME (companyusageevent.domain): un
    // literal propio en vez de importar el dominio de otra feature.
    private static final String ALLTIME = "ALLTIME";

    private final CheckUsageLimitUseCase checkUsageLimit;
    private final RecordCompanyUsageEventUseCase recordUsageEvent;
    private final SystemAuthRunner systemAuthRunner;

    public EntitlementAnimalUsageLimitAdapter(CheckUsageLimitUseCase checkUsageLimit,
            RecordCompanyUsageEventUseCase recordUsageEvent, SystemAuthRunner systemAuthRunner) {
        this.checkUsageLimit = checkUsageLimit;
        this.recordUsageEvent = recordUsageEvent;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void checkNotExceeded(Long companyId) {
        checkUsageLimit.execute(new CheckUsageLimitCommand(companyId, DIMENSION_CODE, ALLTIME));
    }

    @Override
    public void record(Long companyId, Long animalId, LocalDateTime occurredAt) {
        systemAuthRunner
                .run(() -> recordUsageEvent.execute(new RecordCompanyUsageEventCommand(companyId,
                        DIMENSION_CODE, animalId, occurredAt, ALLTIME, false, null)));
    }
}
