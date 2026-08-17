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

    /**
     * Estados que <strong>no</strong> ocupan la agenda del veterinario y por tanto
     * no cuentan como cruce (BE-17).
     *
     * <p>
     * Ojo: no es lo mismo que {@link #isTerminal()}. {@code COMPLETED} también es
     * terminal, pero sí ocupó el hueco del veterinario, así que <em>sí</em> cuenta
     * como choque — mismo criterio que la consulta original, que solo excluía
     * {@code CANCELLED} y {@code NO_SHOW}.
     */
    private static final Set<AppointmentStatus> NOT_OCCUPYING_SCHEDULE = EnumSet.of(CANCELLED,
            NO_SHOW);

    public boolean canTransitionTo(AppointmentStatus next) {
        return next != null && TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    /** {@code true} si la cita reserva el hueco del veterinario. */
    public boolean occupiesSchedule() {
        return !NOT_OCCUPYING_SCHEDULE.contains(this);
    }

    /**
     * Nombres de los estados que no ocupan agenda, para pasarlos como parámetro a
     * la consulta de solapes en vez de repetirlos como literales en el JPQL.
     */
    public static Set<String> namesNotOccupyingSchedule() {
        return NOT_OCCUPYING_SCHEDULE.stream().map(AppointmentStatus::name)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean isTerminal() {
        return TRANSITIONS.getOrDefault(this, Set.of()).isEmpty();
    }
}
