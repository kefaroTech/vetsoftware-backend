package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;

/**
 * R3: la suma de lo aplicado desde un origen —un pago o una nota crédito— nunca
 * puede superar ese origen.
 *
 * <p>
 * <strong>Por qué no la impone la base:</strong> comprobarlo exige agregar
 * filas de {@code billing_document_applications} y compararlas con una fila de
 * otra tabla, y un {@code CHECK} de MySQL no admite subconsultas ni columnas de
 * otras tablas.
 *
 * <p>
 * <strong>Qué se rompe si falla:</strong> la cartera cuadra con plata que no
 * entró, o con un crédito que se gastó dos veces. Los dos son descuadres que
 * solo aparecen en la conciliación del mes siguiente.
 */
public class OverAppliedSourceException extends RuntimeException {
    private final BigDecimal available;
    private final BigDecimal requested;

    public OverAppliedSourceException(ApplicationSourceKind sourceKind, Long sourceId,
            BigDecimal available, BigDecimal requested) {
        super("Applying " + requested + " from " + sourceKind + " " + sourceId
                + " exceeds its available amount of " + available);
        this.available = available;
        this.requested = requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
