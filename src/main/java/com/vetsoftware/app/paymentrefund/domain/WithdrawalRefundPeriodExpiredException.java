package com.vetsoftware.app.paymentrefund.domain;

import java.time.LocalDateTime;

/**
 * El derecho de retracto (Ley 1480/2011 art. 47, ventas a distancia) vence a
 * los cinco dias habiles del cobro. Registrar una devolucion {@code WITHDRAWAL}
 * despues de esa fecha no es un retracto: es otro motivo -correccion, nota
 * credito, castigo- que el operador esta clasificando mal. 409.
 */
public class WithdrawalRefundPeriodExpiredException extends RuntimeException {

    private final Long paymentId;

    public WithdrawalRefundPeriodExpiredException(Long paymentId, LocalDateTime deadline) {
        super("El plazo legal de cinco dias habiles para el retracto de este pago vencio el "
                + deadline + "; registra la devolucion con otro motivo.");
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
