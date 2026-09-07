package com.vetsoftware.app.paymentgateway.application.port.out;

import com.vetsoftware.app.paymentgateway.domain.FirstPeriodChargeOutcome;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservation;
import com.vetsoftware.app.paymentgateway.domain.PaymentReservationOutcome;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * El lado de tesorería del cobro, delegando en {@code subscriptionpayment}:
 * registrar el pago, aplicarlo a la factura, y confirmarlo o marcarlo fallido
 * cuando la pasarela resuelve el estado final.
 */
public interface SubscriptionPaymentLedgerPort {

    /**
     * El pago ya registrado con esta llave de idempotencia (R13), si existe.
     *
     * <p>
     * {@code GatewayCharger.charge} lo consulta <strong>antes</strong> de llamar a
     * la pasarela: si otra instancia ya reservó o cobró esta referencia, el
     * llamador no debe volver a cargar la misma tarjeta.
     */
    Optional<PaymentReservation> findByClientRequestId(Long companyId, String clientRequestId);

    /**
     * Registra el pago (nace {@code PENDING}) y lo aplica al documento.
     *
     * <p>
     * <strong>Idempotente frente a la carrera de inserción</strong>: si dos
     * llamadas concurrentes con el mismo {@code clientRequestId} pasan las dos el
     * chequeo de {@link #findByClientRequestId}, la perdedora del índice único
     * recupera el pago que ganó la carrera
     * -{@link PaymentReservationOutcome#recovered()} en vez de propagar el error de
     * duplicado, y quien la reciba no debe volver a cargar la tarjeta contra la
     * pasarela.
     */
    PaymentReservationOutcome registerAndApply(Long companyId, BigDecimal amount, String currency,
            String gatewayReference, LocalDateTime receivedAt, String clientRequestId,
            Long documentId);

    /**
     * Asigna la referencia real a una reserva {@code PENDING}; una segunda
     * asignación lanza (ver {@code SubscriptionPayment#assignGatewayReference}).
     */
    void assignGatewayReference(Long paymentId, Long companyId, String gatewayReference,
            LocalDateTime gatewayCreatedAt);

    void confirm(Long paymentId, Long companyId);

    void fail(Long paymentId, Long companyId);

    /**
     * La factura a la que se aplicó este pago, para poder anotar el intento
     * fallido.
     */
    Optional<Long> findDocumentIdByPayment(Long companyId, Long paymentId);

    /**
     * El desenlace ya registrado para este pago si ya está en un estado final
     * ({@code CONFIRMED} → {@link FirstPeriodChargeOutcome#APPROVED},
     * {@code FAILED}/{@code REFUNDED} → {@link FirstPeriodChargeOutcome#DECLINED}).
     * Vacío mientras siga {@code PENDING}. Sirve para que
     * {@code GatewayOutcomeSettler.settle} sea idempotente cuando el webhook y el
     * sondeo compiten por el mismo pago.
     */
    Optional<FirstPeriodChargeOutcome> currentOutcome(Long paymentId, Long companyId);
}
