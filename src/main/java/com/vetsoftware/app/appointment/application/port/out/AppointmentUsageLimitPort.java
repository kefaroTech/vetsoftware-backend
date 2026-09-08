package com.vetsoftware.app.appointment.application.port.out;

import java.time.LocalDateTime;

/**
 * Techo gratuito del eje {@code APPOINTMENT}, mismo patrón que
 * {@code BranchCapacityPort}. A diferencia de ANIMAL/OWNER, es {@code FLOW}: el
 * contador se reinicia cada mes.
 */
public interface AppointmentUsageLimitPort {

    void checkNotExceeded(Long companyId);

    void record(Long companyId, Long appointmentId, LocalDateTime occurredAt);
}
