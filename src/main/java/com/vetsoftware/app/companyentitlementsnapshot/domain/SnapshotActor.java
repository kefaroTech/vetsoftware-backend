package com.vetsoftware.app.companyentitlementsnapshot.domain;

/**
 * Quién disparó el recálculo: el proceso automático, una persona de plataforma
 * o un empleado de la clínica.
 *
 * <p>
 * <strong>Exactamente uno</strong>, y lo comprueba también el motor. El diseño
 * original escribía «actor» como texto libre, que es una referencia que nadie
 * puede cruzar: con esto se puede preguntar quién tocó qué.
 */
public record SnapshotActor(Long employeeId, Long systemUserId, boolean process) {

    public SnapshotActor {
        int filled = (employeeId != null ? 1 : 0) + (systemUserId != null ? 1 : 0)
                + (process ? 1 : 0);
        if (filled != 1)
            throw new IllegalArgumentException("exactly one actor must be set (employee,"
                    + " platform user or process) but " + filled + " were");
    }

    public static SnapshotActor employee(Long employeeId) {
        return new SnapshotActor(employeeId, null, false);
    }

    public static SnapshotActor systemUser(Long systemUserId) {
        return new SnapshotActor(null, systemUserId, false);
    }

    public static SnapshotActor automatedProcess() {
        return new SnapshotActor(null, null, true);
    }
}
