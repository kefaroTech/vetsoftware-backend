package com.vetsoftware.app.medicationschedule.domain;

/**
 * Alcance de una reprogramacion. Antes viajaba como {@code String} libre y se
 * comparaba con {@code "cascade".equalsIgnoreCase(...)}: cualquier otro texto
 * —incluido un {@code "cascada"} mal escrito— degradaba a {@link #ONE} en
 * silencio y devolvia 200 (#134). Como enum, el valor desconocido lo rechaza el
 * binder con un 400.
 */
public enum RescheduleMode {
    /** Mueve solo la toma indicada. */
    ONE,
    /**
     * Mueve la toma indicada y, si la pauta lo permite, arrastra las siguientes
     * pendientes. Cuando no lo permite, el resultado dice por que — ver
     * {@link CascadeSkipReason}.
     */
    CASCADE
}
