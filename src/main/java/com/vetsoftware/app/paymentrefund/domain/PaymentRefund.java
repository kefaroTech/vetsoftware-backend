package com.vetsoftware.app.paymentrefund.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La plata que se devuelve, como <strong>documento propio</strong>.
 *
 * <p>
 * Antes devolver dinero era cambiarle el estado a un pago: un {@code UPDATE}
 * sobre una tabla que el modelo declara inmutable, y que ademas borraba cuando,
 * cuanto y por que medio se devolvio. Esa fecha importa dos veces -es la que
 * cuenta para la ventana de arrepentimiento y la que fija el periodo en que se
 * corrige el impuesto- y no se guardaba en ninguna parte.
 *
 * <p>
 * <strong>Solo se agrega.</strong> Todos los campos son {@code final} y no hay
 * un solo mutador, que es la contrapartida en Java de una tabla sin
 * {@code enabled} y sin {@code version}. Una devolucion mal registrada no se
 * edita ni se oculta: se compensa con otra fila, y las dos quedan.
 *
 * <p>
 * <strong>Sacar plata exige firma.</strong> {@code authorizedBySystemUserId} es
 * obligatorio, y no es burocracia: el modelo ya pedia firma para subirle el
 * techo a un cliente y no la pedia para devolverle dinero.
 *
 * <p>
 * <strong>Parcial y varias veces sobre el mismo pago</strong>, por eso no hay
 * unicidad por {@code payment_id}. El tope de la suma lo comprueba
 * {@link #register} porque la base no puede: ver
 * {@link RefundExceedsPaymentAmountException}.
 */
public class PaymentRefund {

    private static final int MAX_DESTINATION_REFERENCE_LENGTH = 120;
    private static final int MAX_REASON_LENGTH = 255;
    private static final int MAX_CLIENT_REQUEST_ID_LENGTH = 64;

    private final Long id;
    private final Long companyId;
    private final Long paymentId;

    /** El documento de cobro que origino la devolucion, si lo hubo. */
    private final Long sourceDocumentId;

    private final BigDecimal amount;
    private final RefundMethod method;
    private final String destinationReference;

    /** Cuando salio el dinero de verdad, que no siempre es cuando se registro. */
    private final LocalDateTime refundedAt;

    /** La fecha que decide en que periodo cae el hecho a efectos de impuesto. */
    private final LocalDate valueDate;

    private final RefundReasonCode reasonCode;
    private final String reason;
    private final Long authorizedBySystemUserId;
    private final String clientRequestId;
    private final LocalDateTime createdDate;

    public PaymentRefund(Long id, Long companyId, Long paymentId, Long sourceDocumentId,
            BigDecimal amount, RefundMethod method, String destinationReference,
            LocalDateTime refundedAt, LocalDate valueDate, RefundReasonCode reasonCode,
            String reason, Long authorizedBySystemUserId, String clientRequestId,
            LocalDateTime createdDate) {
        validate(companyId, paymentId, amount, method, destinationReference, refundedAt, valueDate,
                reasonCode, reason, authorizedBySystemUserId, clientRequestId);
        this.id = id;
        this.companyId = companyId;
        this.paymentId = paymentId;
        this.sourceDocumentId = sourceDocumentId;
        this.amount = amount;
        this.method = method;
        this.destinationReference = destinationReference;
        this.refundedAt = refundedAt;
        this.valueDate = valueDate;
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.authorizedBySystemUserId = authorizedBySystemUserId;
        this.clientRequestId = clientRequestId;
        this.createdDate = createdDate;
    }

    /**
     * Devolucion nueva sobre un pago concreto.
     *
     * <p>
     * <strong>Aqui vive la regla que la base no puede expresar</strong>: la suma de
     * las devoluciones de un pago no supera el pago, ni sumando parciales. Recibe
     * los dos hechos que hay que consultar -el pago y lo ya devuelto- y decide con
     * ellos, en vez de dejar la decision en el service: es una invariante del
     * negocio, no un paso del caso de uso.
     *
     * <p>
     * El {@link SubscriptionPaymentRef} <strong>no se guarda</strong>. Se usa para
     * validar y se descarta; la fila solo conserva el {@code paymentId}, que es lo
     * que la columna almacena.
     *
     * @param alreadyRefunded
     *            suma de lo ya devuelto sobre ese pago. El service la lee
     *            <em>despues</em> de bloquear la fila del pago, porque leerla sin
     *            bloqueo deja pasar dos devoluciones concurrentes
     */
    public static PaymentRefund register(SubscriptionPaymentRef payment, BigDecimal alreadyRefunded,
            Long sourceDocumentId, BigDecimal amount, RefundMethod method,
            String destinationReference, LocalDateTime refundedAt, LocalDate valueDate,
            RefundReasonCode reasonCode, String reason, Long authorizedBySystemUserId,
            String clientRequestId, LocalDateTime createdDate) {
        if (payment == null)
            throw new IllegalArgumentException("payment is required");
        if (amount == null)
            throw new IllegalArgumentException("amount is required");
        BigDecimal refunded = alreadyRefunded == null ? BigDecimal.ZERO : alreadyRefunded;
        if (refunded.add(amount).compareTo(payment.amount()) > 0)
            throw new RefundExceedsPaymentAmountException(payment.id(), payment.amount(), refunded,
                    amount);
        return new PaymentRefund(null, payment.companyId(), payment.id(), sourceDocumentId, amount,
                method, destinationReference, refundedAt, valueDate, reasonCode, reason,
                authorizedBySystemUserId, clientRequestId, createdDate);
    }

    /** Lo devuelto con cargo al saldo a favor no sale de la caja. */
    public boolean movesCash() {
        return method != RefundMethod.CUSTOMER_CREDIT;
    }

    private static void validate(Long companyId, Long paymentId, BigDecimal amount,
            RefundMethod method, String destinationReference, LocalDateTime refundedAt,
            LocalDate valueDate, RefundReasonCode reasonCode, String reason,
            Long authorizedBySystemUserId, String clientRequestId) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (paymentId == null)
            throw new IllegalArgumentException("paymentId is required");
        // Espejo de chk_payment_refunds_amount.
        if (amount == null)
            throw new IllegalArgumentException("amount is required");
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be greater than zero");
        if (method == null)
            throw new IllegalArgumentException("method is required");
        // Espejo de chk_payment_refunds_destination: devolver al saldo a favor no
        // tiene cuenta destino, y devolver por cualquier otra via no se puede
        // registrar sin ella, o el dinero sale sin rastro de adonde fue.
        validateDestination(method, destinationReference);
        if (refundedAt == null)
            throw new IllegalArgumentException("refundedAt is required");
        if (valueDate == null)
            throw new IllegalArgumentException("valueDate is required");
        if (reasonCode == null)
            throw new IllegalArgumentException("reasonCode is required");
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reason is required");
        if (reason.length() > MAX_REASON_LENGTH)
            throw new IllegalArgumentException("reason must be 255 chars or less");
        if (authorizedBySystemUserId == null)
            throw new IllegalArgumentException("authorizedBySystemUserId is required");
        if (clientRequestId != null && clientRequestId.length() > MAX_CLIENT_REQUEST_ID_LENGTH)
            throw new IllegalArgumentException("clientRequestId must be 64 chars or less");
    }

    private static void validateDestination(RefundMethod method, String destinationReference) {
        if (method == RefundMethod.CUSTOMER_CREDIT) {
            if (destinationReference != null)
                throw new IllegalArgumentException(
                        "destinationReference must be absent when refunding to customer credit");
            return;
        }
        if (destinationReference == null || destinationReference.isBlank())
            throw new IllegalArgumentException(
                    "destinationReference is required unless refunding to customer credit");
        if (destinationReference.length() > MAX_DESTINATION_REFERENCE_LENGTH)
            throw new IllegalArgumentException("destinationReference must be 120 chars or less");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundMethod getMethod() {
        return method;
    }

    public String getDestinationReference() {
        return destinationReference;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public RefundReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public Long getAuthorizedBySystemUserId() {
        return authorizedBySystemUserId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
