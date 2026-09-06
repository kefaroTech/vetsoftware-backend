package com.vetsoftware.app.paymentgateway.domain;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * La escalera de reintento de un cobro rechazado {@code SOFT}: 1, 2 y 4 días
 * desde el rechazo anterior —día 1, 3 y 7 desde el primero—, y sin siguiente al
 * cuarto intento imputable (informe {@code proporsal/05-pasarelas-de-pago}).
 *
 * <p>
 * {@code MAX_SOFT_ATTEMPTS} y {@code RETRY_WINDOW} son el espejo de
 * {@code paymentattempt.domain.PaymentAttempt}: el vertical slicing prohíbe
 * importar el dominio de otra rodaja, así que los dos valores se repiten aquí a
 * propósito.
 */
public final class RetrySchedule {

    public static final int MAX_SOFT_ATTEMPTS = 4;
    public static final Duration RETRY_WINDOW = Duration.ofDays(14);

    private static final int[] STEP_DAYS = {1, 2, 4};

    private RetrySchedule() {
    }

    /**
     * @param priorAttempts
     *            cuántos intentos imputables ya se gastaron en la ventana de 14
     *            días, antes de este rechazo
     * @return el instante del siguiente reintento, o {@code null} si el presupuesto
     *         ya se agotó
     */
    public static LocalDateTime nextAttemptAt(int priorAttempts, LocalDateTime now) {
        if (priorAttempts < 0 || priorAttempts >= STEP_DAYS.length) {
            return null;
        }
        return now.plusDays(STEP_DAYS[priorAttempts]);
    }
}
