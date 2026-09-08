package com.vetsoftware.app.daycare.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareUsageLimitPort;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Gemelo exacto de {@code EntitlementSpaUsageLimitAdapter}: GROOMING_SERVICE es
 * {@code FLOW} y mide spa + guardería juntos.
 */
@Component
public class EntitlementDayCareUsageLimitAdapter implements DayCareUsageLimitPort {

    private static final String DIMENSION_CODE = "GROOMING_SERVICE";
    private static final String USAGE_ORIGIN = "DAYCARE";
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckUsageLimitUseCase checkUsageLimit;
    private final RecordCompanyUsageEventUseCase recordUsageEvent;
    private final SystemAuthRunner systemAuthRunner;
    private final Clock clock;

    public EntitlementDayCareUsageLimitAdapter(CheckUsageLimitUseCase checkUsageLimit,
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
    public void record(Long companyId, Long dayCareId, LocalDateTime occurredAt) {
        systemAuthRunner.run(() -> recordUsageEvent
                .execute(new RecordCompanyUsageEventCommand(companyId, DIMENSION_CODE, dayCareId,
                        occurredAt, currentMonthKey(), false, USAGE_ORIGIN)));
    }

    private String currentMonthKey() {
        return YearMonth.now(clock).format(MONTH_KEY);
    }
}
