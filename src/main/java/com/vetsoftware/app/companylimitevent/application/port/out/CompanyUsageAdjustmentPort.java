package com.vetsoftware.app.companylimitevent.application.port.out;

/**
 * Mueve el consumo de un contador.
 *
 * <p>
 * Es un puerto y no una llamada directa porque el contador vive en otra rodaja:
 * la corrección de plataforma no puede importar su caso de uso sin romper el
 * vertical slicing. El adaptador que lo implementa vive en
 * {@code infrastructure/orchestration}, que es el único sitio de esta feature
 * autorizado a conocer la otra.
 *
 * <p>
 * <strong>El movimiento es atómico en el motor</strong> —sube y comprueba el
 * techo en una sola instrucción—, y esta feature no lo reimplementa: lo reusa.
 * Escribir aquí un segundo mecanismo de conteo sería exactamente la carrera que
 * el primero existe para evitar.
 */
public interface CompanyUsageAdjustmentPort {

    /**
     * @param delta
     *            positivo o negativo. Corregir quinientas mascotas duplicadas de
     *            una migración es un delta de −500
     * @return el consumo que queda después del movimiento
     */
    int adjustUsage(Long companyId, String capacityUnit, int delta);

    /** El techo y el consumo que rigen ahora mismo, para copiarlos en el hecho. */
    UsageSnapshot currentUsage(Long companyId, String capacityUnit);

    /**
     * Los dos números del momento, copiados en el hecho que documenta la
     * corrección.
     */
    record UsageSnapshot(int limitQuantity, int usedQuantity) {
    }
}
