package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code payment_refunds} - la plata que se devuelve.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @Version}</strong>
 * ({@code E1_APPEND_ONLY} en {@code ENTIDADES_EXENTAS_DE_VERSION}): documento
 * de dinero, solo se agrega. Una devolucion mal registrada no se edita ni se
 * oculta, se compensa con otra fila. Sin ediciones no hay dos escrituras que
 * puedan pisarse, que es lo unico que {@code @Version} protege; y con borrado
 * logico, un {@code @SQLRestriction} escondería la mitad del cuadre de caja.
 *
 * <p>
 * <strong>Las FK van como escalares y no como {@code @ManyToOne}</strong>, y es
 * una decision con motivo. Las tres referencias a las tablas del dinero son
 * <em>compuestas</em> {@code (company_id, id)}, asi que la asociacion obligaria
 * a un {@code @JoinColumns} que comparte la columna {@code company_id} con las
 * demas; Hibernate exige que todas las columnas de una propiedad tengan el
 * mismo modo de escritura y solo un mapeo puede ser dueño de una columna
 * fisica, de modo que las asociaciones tendrian que ir todas
 * {@code insertable = false, updatable = false}. Es la trampa que documenta
 * {@code BillingDocumentApplicationJpaEntity}, y ahi el fallo ni siquiera
 * señala a la clase culpable: revienta el {@code entityManagerFactory} y se
 * lleva por delante la aplicacion entera.
 *
 * <p>
 * Sin asociaciones tampoco hay N+1 que evitar ni {@code @EntityGraph} que
 * poner: los datos del pago que esta feature necesita llegan por
 * {@code SubscriptionPaymentQueryPort} en una sola consulta acotada, que es el
 * patron de companion VO del {@code CLAUDE.md}. La FK sigue existiendo y
 * siguiendo vigilando en la base; lo que no existe es la navegacion desde Java.
 */
@Entity
@Table(name = "payment_refunds")
public class PaymentRefundJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 30)
    private RefundMethod method;

    @Column(name = "destination_reference", length = 120)
    private String destinationReference;

    @Column(name = "refunded_at", nullable = false)
    private LocalDateTime refundedAt;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private RefundReasonCode reasonCode;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "authorized_by_system_user_id", nullable = false)
    private Long authorizedBySystemUserId;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected PaymentRefundJpaEntity() {
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

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(Long sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public RefundMethod getMethod() {
        return method;
    }

    public void setMethod(RefundMethod method) {
        this.method = method;
    }

    public String getDestinationReference() {
        return destinationReference;
    }

    public void setDestinationReference(String destinationReference) {
        this.destinationReference = destinationReference;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public RefundReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(RefundReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getAuthorizedBySystemUserId() {
        return authorizedBySystemUserId;
    }

    public void setAuthorizedBySystemUserId(Long authorizedBySystemUserId) {
        this.authorizedBySystemUserId = authorizedBySystemUserId;
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
}
