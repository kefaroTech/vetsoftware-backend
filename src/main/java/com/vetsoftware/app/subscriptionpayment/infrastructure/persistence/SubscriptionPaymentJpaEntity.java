package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code subscription_payments}.
 *
 * <p>
 * <strong>Sin {@code enabled}, y por tanto sin {@code @SQLDelete} ni
 * {@code @SQLRestriction}.</strong> Un pago que entro, entro: si se devolvio es
 * {@code status = 'REFUNDED'}, no una fila invisible. Con borrado logico, el
 * {@code @SQLRestriction} escondería la mitad de la conciliacion y el saldo
 * dejaria de poder reconstruirse.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: el estado y la fecha de conciliacion
 * si mutan, y dos operarios confirmando a la vez se pisarian sin ruido.
 */
@Entity
@Table(name = "subscription_payments")
public class SubscriptionPaymentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Escalar y no una asociacion a {@code CompanyJpaEntity}, por dos motivos que
     * se refuerzan: es la forma que usan las otras tablas de dinero del bloque
     * ({@code subscription_billing_documents}), y hace que el
     * {@code referencedColumnName = "company_id"} con el que
     * {@code billing_document_applications} apunta al par {@code (company_id, id)}
     * de esta tabla resuelva contra una propiedad basica, que es el camino robusto
     * en Hibernate.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // columnDefinition explicito: la columna es CHAR(3) y sin el Hibernate espera
    // varchar, lo que rompe ddl-auto: validate con un mismatch de tipo JDBC.
    // Mismo criterio que PriceListJpaEntity.
    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "gateway", length = 40)
    private String gateway;

    @Column(name = "gateway_reference", length = 120)
    private String gatewayReference;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionPaymentStatus status;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SubscriptionPaymentJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public SubscriptionPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionPaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
