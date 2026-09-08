package com.vetsoftware.app.appointment.infrastructure.orchestration;

import com.vetsoftware.app.appointment.application.port.out.AppointmentUsageLimitPort;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companyusageevent.application.command.CheckUsageLimitCommand;
import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.port.in.CheckUsageLimitUseCase;
import com.vetsoftware.app.companyusageevent.application.port.in.RecordCompanyUsageEventUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * APPOINTMENT es {@code FLOW}: el techo gratuito es mensual, así que la clave
 * de periodo (mes en curso) hay que recalcularla en cada llamada, no congelarla
 * en una constante.
 */
@Component
public class EntitlementAppointmentUsageLimitAdapter implements AppointmentUsageLimitPort {

    private static final String DIMENSION_CODE = "APPOINTMENT";
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CheckUsageLimitUseCase checkUsageLimit;
    private final RecordCompanyUsageEventUseCase recordUsageEvent;
    private final SystemAuthRunner systemAuthRunner;
    private final Clock clock;

    public EntitlementAppointmentUsageLimitAdapter(CheckUsageLimitUseCase checkUsageLimit,
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
    public void record(Long companyId, Long appointmentId, LocalDateTime occurredAt) {
        systemAuthRunner.run(() -> recordUsageEvent
                .execute(new RecordCompanyUsageEventCommand(companyId, DIMENSION_CODE,
                        appointmentId, occurredAt, currentMonthKey(), false, null)));
    }

    private String currentMonthKey() {
        return YearMonth.now(clock).format(MONTH_KEY);
    }
}
