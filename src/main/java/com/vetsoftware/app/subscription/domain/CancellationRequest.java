package com.vetsoftware.app.subscription.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Las dos fechas de una cancelacion, que <strong>no son la misma</strong>: el
 * cliente cancela el 10 y se va el 30, que es lo que ya pago.
 *
 * <p>
 * Separarlas es lo que evita los dos errores simetricos: cortarle el servicio
 * el dia 10 —que es cobrarle veinte dias que no disfruta— o seguir facturandole
 * despues del 30 porque nadie apunto cuando pidio irse. {@code cancel_reason}
 * se guarda porque es informacion de negocio: es la unica fuente que dice por
 * que se van los clientes.
 *
 * <p>
 * Los dos campos van juntos o no va ninguno, que es exactamente lo que impone
 * {@code chk_subscriptions_cancel}.
 *
 * @param requestedAt
 *            cuando lo pidio
 * @param effectiveDate
 *            cuando surte efecto
 * @param reason
 *            por que se va
 */
public record CancellationRequest(LocalDateTime requestedAt, LocalDate effectiveDate,
        String reason) {

    public static final int MAX_REASON_LENGTH = 255;

    public CancellationRequest {
        if (requestedAt == null)
            throw new IllegalArgumentException("cancelRequestedAt is required");
        if (effectiveDate == null)
            throw new IllegalArgumentException("cancelEffectiveDate is required");
        if (effectiveDate.isBefore(requestedAt.toLocalDate()))
            throw new IllegalArgumentException(
                    "cancelEffectiveDate must not be before cancelRequestedAt");
        if (reason != null && reason.length() > MAX_REASON_LENGTH)
            throw new IllegalArgumentException(
                    "cancelReason must be " + MAX_REASON_LENGTH + " chars or less");
    }

    /** ¿La baja ya surtio efecto ese dia? El dia efectivo todavia esta cubierto. */
    public boolean hasTakenEffectOn(LocalDate day) {
        if (day == null)
            throw new IllegalArgumentException("day is required");
        return !day.isBefore(effectiveDate);
    }
}
