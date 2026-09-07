package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La plata que entro, <strong>independiente de a que factura se
 * aplique</strong>.
 *
 * <p>
 * Estan separados a proposito: un cliente puede pagar tres facturas de un giro,
 * o abonar la mitad de una. Quien salda que lo dice
 * {@link BillingDocumentApplication}, no esta fila.
 *
 * <p>
 * <strong>R1 - un pago nunca cambia de importe tras crearse.</strong>
 * {@code amount}, {@code currency} y {@code paymentMethod} son {@code final} y
 * no tienen mutador. {@code status} muta por la tabla de transiciones de
 * {@link SubscriptionPaymentStatus}, {@code reconciledAt} por
 * {@link #reconcile} y {@code gatewayReference} se escribe como mucho una vez,
 * con {@link #assignGatewayReference}: una reserva de pasarela nace
 * {@code PENDING} sin referencia -el {@code POST /transactions} aun no
 * respondio- y la recibe cuando la pasarela confirma. Corregir un pago mal
 * registrado es registrar otro que lo compensa, nunca editar este.
 *
 * <p>
 * <strong>{@code receivedAt} es la unica excepcion a "final", y solo por la
 * misma via.</strong> Nace con la fecha de la reserva, que todavia no es un
 * cobro real; cuando {@link #assignGatewayReference} recibe la fecha de la
 * transaccion de la pasarela, la sustituye por esa: la fecha contable de un
 * cobro es la de la transaccion real, no la de la reserva que la precedio.
 */
public class SubscriptionPayment {

    /** Longitud de {@code subscription_payments.currency}: codigo ISO-4217. */
    private static final int CURRENCY_LENGTH = 3;
    private static final int MAX_GATEWAY_LENGTH = 40;
    private static final int MAX_GATEWAY_REFERENCE_LENGTH = 120;
    private static final int MAX_SETTLEMENT_REFERENCE_LENGTH = 120;
    private static final int MAX_CLIENT_REQUEST_ID_LENGTH = 64;

    private final Long id;
    private final Long companyId;
    private final BigDecimal amount;
    private final String currency;
    private final PaymentMethod paymentMethod;
    private final String gateway;
    private String gatewayReference;

    /** Cuando entro de verdad, que no siempre es cuando se registro. */
    private LocalDateTime receivedAt;

    private SubscriptionPaymentStatus status;
    private LocalDateTime reconciledAt;

    /**
     * Lo que se queda la pasarela. {@code null} hasta que {@link #settle} lo
     * registre.
     */
    private BigDecimal feeAmount;

    /** Lo que de verdad entro a la cuenta: {@code amount - feeAmount}. */
    private BigDecimal netAmount;

    /** El lote de liquidacion de la pasarela que trajo este pago. */
    private String settlementReference;

    /** Cuando la pasarela liquido este pago, que no es cuando se recibio. */
    private LocalDate settledOn;

    /**
     * Lo devuelto contra este pago, via {@code payment_refunds}. Nunca negativo.
     */
    private final BigDecimal refundedAmount;

    private final String clientRequestId;
    private final LocalDateTime createdDate;
    private final Long version;

    public SubscriptionPayment(Long id, Long companyId, BigDecimal amount, String currency,
            PaymentMethod paymentMethod, String gateway, String gatewayReference,
            LocalDateTime receivedAt, SubscriptionPaymentStatus status, LocalDateTime reconciledAt,
            BigDecimal feeAmount, BigDecimal netAmount, String settlementReference,
            LocalDate settledOn, BigDecimal refundedAmount, String clientRequestId,
            LocalDateTime createdDate, Long version) {
        validate(companyId, amount, currency, paymentMethod, gateway, gatewayReference, receivedAt,
                status, reconciledAt, settlementReference, refundedAmount, clientRequestId);
        this.id = id;
        this.companyId = companyId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.gateway = gateway;
        this.gatewayReference = gatewayReference;
        this.receivedAt = receivedAt;
        this.status = status;
        this.reconciledAt = reconciledAt;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.settlementReference = settlementReference;
        this.settledOn = settledOn;
        this.refundedAmount = refundedAmount;
        this.clientRequestId = clientRequestId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Pago recien recibido. Nace {@code PENDING} porque registrar no es cobrar:
     * hasta que la pasarela o el operador lo confirmen, esta plata no reduce el
     * saldo de ninguna factura.
     */
    public static SubscriptionPayment register(Long companyId, BigDecimal amount, String currency,
            PaymentMethod paymentMethod, String gateway, String gatewayReference,
            LocalDateTime receivedAt, String clientRequestId, LocalDateTime createdDate) {
        return new SubscriptionPayment(null, companyId, amount, currency, paymentMethod, gateway,
                gatewayReference, receivedAt, SubscriptionPaymentStatus.PENDING, null, null, null,
                null, null, BigDecimal.ZERO, clientRequestId, createdDate, null);
    }

    /**
     * Registra la conciliacion de la pasarela: su comision, el neto que de verdad
     * entro y la referencia del lote de liquidacion.
     *
     * <p>
     * Solo un pago {@code CONFIRMED} se concilia -mismo criterio que
     * {@link #reconcile}- y es <strong>idempotente</strong>: conciliar dos veces
     * conserva los primeros datos en vez de reescribir el pasado, que es lo que R1
     * prohibe.
     */
    public void settle(BigDecimal feeAmount, BigDecimal netAmount, String settlementReference,
            LocalDate settledOn) {
        if (status != SubscriptionPaymentStatus.CONFIRMED)
            throw new SubscriptionPaymentNotConfirmedException(id);
        if (settlementReference != null
                && settlementReference.length() > MAX_SETTLEMENT_REFERENCE_LENGTH)
            throw new IllegalArgumentException("settlementReference must be 120 chars or less");
        if (this.settlementReference != null)
            return;
        this.feeAmount = feeAmount;
        this.netAmount = netAmount;
        this.settlementReference = settlementReference;
        this.settledOn = settledOn;
    }

    /**
     * Mueve el estado respetando la tabla de transiciones. Una transicion prohibida
     * lanza {@link InvalidSubscriptionPaymentStatusTransitionException} (409),
     * nunca se ignora en silencio.
     *
     * <p>
     * <strong>Confirmar un pago de pasarela exige que ya tenga referencia.
     * </strong> Sin ella, un {@code CONFIRMED} es indistinguible de un ingreso
     * fantasma: cuenta como cobro (R4) y no hay con que cruzarlo contra el extracto
     * de Wompi. Un pago sin pasarela ({@code gateway == null}, registro manual por
     * transferencia o efectivo) no lleva referencia nunca y se confirma igual.
     */
    public void changeStatus(SubscriptionPaymentStatus target) {
        if (target == null)
            throw new IllegalArgumentException("target status is required");
        if (!status.canTransitionTo(target))
            throw new InvalidSubscriptionPaymentStatusTransitionException(status, target);
        if (target == SubscriptionPaymentStatus.CONFIRMED && gateway != null
                && gatewayReference == null)
            throw new SubscriptionPaymentMissingGatewayReferenceException(id);
        this.status = target;
    }

    /**
     * Asigna la referencia de la pasarela a una reserva que nacio sin ella.
     *
     * <p>
     * Se escribe <strong>una sola vez</strong>: si ya tiene referencia o el pago
     * dejo de estar {@code PENDING}, lanza en vez de sobrescribir -es la mitad de
     * R1 que sigue vigente para este campo.
     *
     * @param gatewayCreatedAt
     *            cuando la pasarela dice que ocurrio la transaccion, o {@code null}
     *            si no la informa.
     */
    public void assignGatewayReference(String gatewayReference, LocalDateTime gatewayCreatedAt) {
        if (gatewayReference == null || gatewayReference.isBlank())
            throw new IllegalArgumentException("gatewayReference is required");
        if (gatewayReference.length() > MAX_GATEWAY_REFERENCE_LENGTH)
            throw new IllegalArgumentException("gatewayReference must be 120 chars or less");
        if (this.gatewayReference != null)
            throw new IllegalStateException("gatewayReference is already assigned");
        if (status != SubscriptionPaymentStatus.PENDING)
            throw new IllegalStateException(
                    "only a PENDING payment can receive a gateway reference");
        this.gatewayReference = gatewayReference;
        if (gatewayCreatedAt != null)
            this.receivedAt = gatewayCreatedAt;
    }

    /**
     * Marca el pago como cuadrado contra el extracto bancario.
     *
     * <p>
     * Solo un pago {@code CONFIRMED} se puede conciliar: es
     * {@code chk_subscription_payments_reconciled} escrito en el dominio. Y es
     * <strong>idempotente</strong>: reconciliar dos veces conserva la primera fecha
     * en lugar de reescribir el pasado, que es lo que R1 prohibe.
     */
    public void reconcile(LocalDateTime at) {
        if (at == null)
            throw new IllegalArgumentException("reconciledAt is required");
        if (status != SubscriptionPaymentStatus.CONFIRMED)
            throw new SubscriptionPaymentNotConfirmedException(id);
        if (reconciledAt == null)
            this.reconciledAt = at;
    }

    /** Solo lo confirmado cuenta como cobro; lo demas no salda nada. */
    public boolean countsAsSettlement() {
        return status == SubscriptionPaymentStatus.CONFIRMED;
    }

    /**
     * Puede aplicarse a una factura: la pasarela avisó (PENDING) o ya confirmó. Un
     * pago que se sabe que no va a llegar ({@code FAILED}/{@code REFUNDED}) no se
     * aplica — aplicar y luego revertir sería reescribir la aplicación, que R1
     * prohíbe. Distinto de {@link #countsAsSettlement()}: ese decide si <em>reduce
     * el saldo</em>, este si <em>puede colgarse</em> del documento.
     */
    public boolean canBeApplied() {
        return status == SubscriptionPaymentStatus.PENDING
                || status == SubscriptionPaymentStatus.CONFIRMED;
    }

    private static void validate(Long companyId, BigDecimal amount, String currency,
            PaymentMethod paymentMethod, String gateway, String gatewayReference,
            LocalDateTime receivedAt, SubscriptionPaymentStatus status, LocalDateTime reconciledAt,
            String settlementReference, BigDecimal refundedAmount, String clientRequestId) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (amount == null)
            throw new IllegalArgumentException("amount is required");
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be greater than zero");
        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("currency is required");
        if (currency.length() != CURRENCY_LENGTH || !currency.equals(currency.toUpperCase()))
            throw new IllegalArgumentException("currency must be a 3-letter uppercase ISO code");
        if (paymentMethod == null)
            throw new IllegalArgumentException("paymentMethod is required");
        // Espejo de chk_subscription_payments_gateway_pair.
        if (gatewayReference != null && gateway == null)
            throw new IllegalArgumentException("gatewayReference requires a gateway");
        if (gateway != null && gateway.length() > MAX_GATEWAY_LENGTH)
            throw new IllegalArgumentException("gateway must be 40 chars or less");
        if (gatewayReference != null && gatewayReference.length() > MAX_GATEWAY_REFERENCE_LENGTH)
            throw new IllegalArgumentException("gatewayReference must be 120 chars or less");
        if (receivedAt == null)
            throw new IllegalArgumentException("receivedAt is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (reconciledAt != null && status != SubscriptionPaymentStatus.CONFIRMED)
            throw new IllegalArgumentException("only a CONFIRMED payment can be reconciled");
        if (settlementReference != null
                && settlementReference.length() > MAX_SETTLEMENT_REFERENCE_LENGTH)
            throw new IllegalArgumentException("settlementReference must be 120 chars or less");
        // Espejo de subscription_payments.refunded_amount, NOT NULL: un pago sin
        // devoluciones lo dice con cero, nunca con un valor ausente.
        if (refundedAmount == null)
            throw new IllegalArgumentException("refundedAmount is required");
        if (refundedAmount.signum() < 0)
            throw new IllegalArgumentException("refundedAmount cannot be negative");
        if (clientRequestId != null && clientRequestId.length() > MAX_CLIENT_REQUEST_ID_LENGTH)
            throw new IllegalArgumentException("clientRequestId must be 64 chars or less");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getGateway() {
        return gateway;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public SubscriptionPaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public String getSettlementReference() {
        return settlementReference;
    }

    public LocalDate getSettledOn() {
        return settledOn;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
