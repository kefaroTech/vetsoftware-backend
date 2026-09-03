package com.vetsoftware.app.medicationschedule.domain;

/**
 * Alcance de una reprogramacion.
 *
 * <p>
 * Es un enum y no un {@code String} libre porque, comparando texto, cualquier
 * valor distinto de {@code "cascade"} —incluido un {@code "cascada"} mal
 * escrito— degrada a {@link #ONE} en silencio y responde 200. Como enum, el
 * valor desconocido lo rechaza el binder con un 400.
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
