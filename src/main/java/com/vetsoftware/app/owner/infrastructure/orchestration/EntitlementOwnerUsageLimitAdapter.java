package com.vetsoftware.app.owner.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerUsageLimitPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** OWNER es {@code CUMULATIVE}: cuenta desde siempre, sin reinicio mensual. */
@Component
public class EntitlementOwnerUsageLimitAdapter implements OwnerUsageLimitPort {

    private static final String DIMENSION_CODE = "OWNER";
    // Espejo del centinela UsagePeriodKey.ALLTIME (companyusageevent.domain): un
    // literal propio en vez de importar el dominio de otra feature.
    private static final String ALLTIME = "ALLTIME";

    private final CheckUsageLimitUseCase checkUsageLimit;
    private final RecordCompanyUsageEventUseCase recordUsageEvent;
    private final SystemAuthRunner systemAuthRunner;

    public EntitlementOwnerUsageLimitAdapter(CheckUsageLimitUseCase checkUsageLimit,
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
    public void record(Long companyId, Long ownerId, LocalDateTime occurredAt) {
        systemAuthRunner
                .run(() -> recordUsageEvent.execute(new RecordCompanyUsageEventCommand(companyId,
                        DIMENSION_CODE, ownerId, occurredAt, ALLTIME, false, null)));
    }
}
