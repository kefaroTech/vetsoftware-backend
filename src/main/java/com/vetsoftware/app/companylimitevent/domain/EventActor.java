package com.vetsoftware.app.companylimitevent.domain;

/**
 * Quién hizo la operación: el empleado de la clínica que la intentó, la persona
 * de plataforma que corrigió el consumo, o el proceso automático.
 *
 * <p>
 * <strong>Exactamente uno</strong>, y lo comprueba también el motor
 * ({@code chk_company_limit_events_actor}). El diseño original escribía «actor»
 * como texto libre, que es una referencia que nadie puede cruzar: con esto,
 * «qué hizo esta persona en marzo» es una consulta.
 */
public record EventActor(Long employeeId, Long systemUserId, boolean process) {

    public EventActor {
        int filled = (employeeId != null ? 1 : 0) + (systemUserId != null ? 1 : 0)
                + (process ? 1 : 0);
        if (filled != 1)
            throw new IllegalArgumentException("exactly one actor must be set (employee,"
                    + " platform user or process) but " + filled + " were");
    }

    /** El empleado de la clínica que intentó la operación. */
    public static EventActor employee(Long employeeId) {
        return new EventActor(employeeId, null, false);
    }

    /** La persona de plataforma que firma la corrección. */
    public static EventActor systemUser(Long systemUserId) {
        return new EventActor(null, systemUserId, false);
    }

    /** El barrido o el recálculo. */
    public static EventActor automatedProcess() {
        return new EventActor(null, null, true);
    }
}
