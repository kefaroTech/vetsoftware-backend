package com.vetsoftware.app.appointment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum AppointmentStatus {
    REQUESTED, CONFIRMED, ARRIVED, IN_PROGRESS, COMPLETED, NO_SHOW, CANCELLED;

    private static final Map<AppointmentStatus, Set<AppointmentStatus>> TRANSITIONS = Map.of(
            REQUESTED, EnumSet.of(CONFIRMED, CANCELLED, NO_SHOW), CONFIRMED,
            EnumSet.of(ARRIVED, CANCELLED, NO_SHOW), ARRIVED,
            EnumSet.of(IN_PROGRESS, CANCELLED, NO_SHOW), IN_PROGRESS,
            EnumSet.of(COMPLETED, CANCELLED), COMPLETED, EnumSet.noneOf(AppointmentStatus.class),
            NO_SHOW, EnumSet.noneOf(AppointmentStatus.class), CANCELLED,
            EnumSet.noneOf(AppointmentStatus.class));

    public boolean canTransitionTo(AppointmentStatus next) {
        return next != null && TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return TRANSITIONS.getOrDefault(this, Set.of()).isEmpty();
    }
}
