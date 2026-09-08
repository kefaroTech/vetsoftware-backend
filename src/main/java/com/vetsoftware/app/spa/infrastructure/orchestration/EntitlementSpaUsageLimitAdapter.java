package com.vetsoftware.app.spa.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaUsageLimitPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * GROOMING_SERVICE es {@code FLOW} y mide spa + guardería juntos (se cuenta al
 * crear, no al completar). {@code daycare} tiene el gemelo exacto de este
 * adaptador; los dos comparten el mismo eje y el mismo mes, así que un spa y
 * una guardería creados el mismo mes compiten por el mismo cupo.
 */
@Component
public class EntitlementSpaUsageLimitAdapter implements SpaUsageLimitPort {

    private static final String DIMENSION_CODE = "GROOMING_SERVICE";
    private static final String USAGE_ORIGIN = "SPA";
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckUsageLimitUseCase checkUsageLimit;
    private final RecordCompanyUsageEventUseCase recordUsageEvent;
    private final SystemAuthRunner systemAuthRunner;
    private final Clock clock;

    public EntitlementSpaUsageLimitAdapter(CheckUsageLimitUseCase checkUsageLimit,
            RecordCompanyUsageEventUseCase recordUsageEvent, SystemAuthRunner systemAuthRunner,
            Clock clock) {
        this.checkUsageLimit = checkUsageLimit;
        this.recordUsageEvent = recordUsageEvent;
        this.systemAuthRunner = systemAuthRunner;
        this.clock = clock;
    }

    @Override
    public void checkNotExceeded(Long companyId) {
        checkUsageLimit
                .execute(new CheckUsageLimitCommand(companyId, DIMENSION_CODE, currentMonthKey()));
    }

    @Override
    public void record(Long companyId, Long spaId, LocalDateTime occurredAt) {
        systemAuthRunner.run(() -> recordUsageEvent
                .execute(new RecordCompanyUsageEventCommand(companyId, DIMENSION_CODE, spaId,
                        occurredAt, currentMonthKey(), false, USAGE_ORIGIN)));
    }

    private String currentMonthKey() {
        return YearMonth.now(clock).format(MONTH_KEY);
    }
}
