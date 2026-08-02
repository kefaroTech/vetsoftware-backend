package com.vetsoftware.app.electronicdocument.application.port.out;

/**
 * Detección automática del "modo contingencia" por empresa: cuando el
 * proveedor/DIAN se cae de forma sostenida (varios fallos de infraestructura
 * seguidos) se ACTIVA solo, y se DESACTIVA solo cuando el proveedor vuelve a
 * responder. Es la pieza 1 del modo contingencia tipo 04 (la emisión que
 * consume este estado es la pieza 2, pendiente de confirmar el flujo offline
 * con MATIAS).
 */
public interface ContingencyMonitorPort {

    /**
     * Registra el resultado de un intento de transmisión.
     *
     * @param providerResponded
     *            {@code true} si el proveedor respondió
     *            (VALIDADO/RECHAZADO/PENDIENTE = sano); {@code false} si fue fallo
     *            de infraestructura (5xx/timeout → CONTINGENCIA).
     */
    void recordOutcome(Long companyId, boolean providerResponded);

    /**
     * ¿La empresa está actualmente en modo contingencia (caída sostenida
     * detectada)?
     */
    boolean isActive(Long companyId);
}
