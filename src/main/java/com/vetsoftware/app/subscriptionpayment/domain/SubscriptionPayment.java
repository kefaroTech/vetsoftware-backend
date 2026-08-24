package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;
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
 * {@code amount}, {@code currency}, {@code paymentMethod}, {@code gateway},
 * {@code gatewayReference} y {@code receivedAt} son {@code final} y no tienen
 * mutador. Lo unico que muta es {@code status} -por la tabla de transiciones de
 * {@link SubscriptionPaymentStatus}- y {@code reconciledAt}. Corregir un pago
 * mal registrado es registrar otro que lo compensa, nunca editar este.
 */
public class SubscriptionPayment {

    /** Longitud de {@code subscription_payments.currency}: codigo ISO-4217. */
    private static final int CURRENCY_LENGTH = 3;
    private static final int MAX_GATEWAY_LENGTH = 40;
    private static final int MAX_GATEWAY_REFERENCE_LENGTH = 120;
    private static final int MAX_CLIENT_REQUEST_ID_LENGTH = 64;

    private final Long id;
    private final Long companyId;
    private final BigDecimal amount;
    private final String currency;
    private final PaymentMethod paymentMethod;
    private final String gateway;
    private final String gatewayReference;

    /** Cuando entro de verdad, que no siempre es cuando se registro. */
    private final LocalDateTime receivedAt;

    private SubscriptionPaymentStatus status;
    private LocalDateTime reconciledAt;

    private final String clientRequestId;
    private final LocalDateTime createdDate;
    private final Long version;

    public SubscriptionPayment(Long id, Long companyId, BigDecimal amount, String currency,
            PaymentMethod paymentMethod, String gateway, String gatewayReference,
            LocalDateTime receivedAt, SubscriptionPaymentStatus status, LocalDateTime reconciledAt,
            String clientRequestId, LocalDateTime createdDate, Long version) {
        validate(companyId, amount, currency, paymentMethod, gateway, gatewayReference, receivedAt,
                status, reconciledAt, clientRequestId);
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
                gatewayReference, receivedAt, SubscriptionPaymentStatus.PENDING, null,
                clientRequestId, createdDate, null);
    }

    /**
     * Mueve el estado respetando la tabla de transiciones. Una transicion prohibida
     * lanza {@link InvalidSubscriptionPaymentStatusTransitionException} (409),
     * nunca se ignora en silencio.
     */
    public void changeStatus(SubscriptionPaymentStatus target) {
        if (target == null)
            throw new IllegalArgumentException("target status is required");
        if (!status.canTransitionTo(target))
            throw new InvalidSubscriptionPaymentStatusTransitionException(status, target);
        this.status = target;
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

    private static void validate(Long companyId, BigDecimal amount, String currency,
            PaymentMethod paymentMethod, String gateway, String gatewayReference,
            LocalDateTime receivedAt, SubscriptionPaymentStatus status, LocalDateTime reconciledAt,
            String clientRequestId) {
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
        // Espejo de chk_subscription_payments_gateway_pair: una pasarela sin
        // referencia no deduplica nada, y una referencia sin pasarela no se puede
        // atribuir a nadie.
        if ((gateway == null) != (gatewayReference == null))
            throw new IllegalArgumentException(
                    "gateway and gatewayReference must be both present or both absent");
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
