package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.infrastructure.persistence.SubscriptionBillingDocumentJpaEntity;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code billing_document_applications} — que salda que.
 *
 * <p>
 * <strong>Las cuatro referencias son claves foraneas COMPUESTAS
 * {@code (company_id, id)}, no simples.</strong> No es purismo: con una FK
 * simple, un pago de la clinica A podia saldar la factura de la clinica B y
 * ninguna revision de codigo lo veia, porque no es un error de programacion
 * sino un hueco del esquema. Arrastrando la empresa dentro de la clave, la base
 * <em>rechaza la fila</em> y deja de ser una regla que hay que recordar.
 *
 * <p>
 * <strong>Los escalares escriben; las asociaciones son de solo
 * lectura.</strong> Hibernate exige que TODAS las columnas de una propiedad
 * compartan el mismo modo de escritura: un {@code @JoinColumns} que mezcle una
 * columna escribible con otra {@code insertable = false} no arranca el contexto
 * ({@code AnnotationException: Column mappings for property '...' mix insertable
 * with 'insertable=false'}), y el fallo no es de esta clase sino del
 * {@code entityManagerFactory}, asi que se lleva por delante la aplicacion
 * entera y no senala aqui.
 *
 * <p>
 * Y {@code company_id} no puede ser escribible en ninguna de las cuatro, porque
 * es la misma columna fisica compartida por todas y solo un mapeo puede ser su
 * dueno. De ahi que la unica combinacion posible sea: los cinco escalares
 * ({@link #companyId}, {@link #targetDocumentId}, {@link #paymentId},
 * {@link #sourceDocumentId}, {@link #reversalOfId}) escriben, y las cuatro
 * asociaciones existen solo para navegar. Los setters mantienen las dos caras
 * en sincronia.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @Version}</strong>
 * ({@code E1_APPEND_ONLY}): una aplicacion no se edita ni se desactiva. Si esta
 * mal se contra-aplica con otra fila negativa que apunta a ella, y las dos
 * quedan. Desactivarla haria el saldo irreconstruible.
 */
@Entity
@Table(name = "billing_document_applications")
public class BillingDocumentApplicationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Duena de la escritura de {@code company_id}. Las cuatro asociaciones de abajo
     * mapean la misma columna en solo lectura: es una unica columna fisica y solo
     * un mapeo puede escribirla.
     */
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    /** La factura cuyo saldo se reduce. Par P3 del inventario de FK compuestas. */
    @Column(name = "target_document_id", nullable = false)
    private Long targetDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "target_document_id", referencedColumnName = "id", insertable = false, updatable = false)})
    private SubscriptionBillingDocumentJpaEntity targetDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 20)
    private ApplicationSourceKind sourceKind;

    /** El origen si es un pago. Par P4. Excluyente con {@link #sourceDocument}. */
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "payment_id", referencedColumnName = "id", insertable = false, updatable = false)})
    private SubscriptionPaymentJpaEntity payment;

    /** El origen si es una nota credito. Excluyente con {@link #payment}. */
    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "source_document_id", referencedColumnName = "id", insertable = false, updatable = false)})
    private SubscriptionBillingDocumentJpaEntity sourceDocument;

    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal appliedAmount;

    /** Autorreferencia compuesta: la aplicacion que esta fila contra-aplica. */
    @Column(name = "reversal_of_id")
    private Long reversalOfId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "company_id", referencedColumnName = "company_id", insertable = false, updatable = false),
            @JoinColumn(name = "reversal_of_id", referencedColumnName = "id", insertable = false, updatable = false)})
    private BillingDocumentApplicationJpaEntity reversalOf;

    /**
     * Llave de idempotencia (R13), unica por empresa. Nula en las reversas, que se
     * deduplican por {@code uq_bda_reversal}, y nula tambien cuando el cliente no
     * manda ninguna: MySQL admite multiples {@code NULL} en un indice unico, asi
     * que esas filas no colisionan entre si.
     */
    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected BillingDocumentApplicationJpaEntity() {
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

    public SubscriptionBillingDocumentJpaEntity getTargetDocument() {
        return targetDocument;
    }

    public void setTargetDocument(SubscriptionBillingDocumentJpaEntity targetDocument) {
        this.targetDocument = targetDocument;
        this.targetDocumentId = targetDocument == null ? null : targetDocument.getId();
    }

    public ApplicationSourceKind getSourceKind() {
        return sourceKind;
    }

    public void setSourceKind(ApplicationSourceKind sourceKind) {
        this.sourceKind = sourceKind;
    }

    public SubscriptionPaymentJpaEntity getPayment() {
        return payment;
    }

    public void setPayment(SubscriptionPaymentJpaEntity payment) {
        this.payment = payment;
        this.paymentId = payment == null ? null : payment.getId();
    }

    public SubscriptionBillingDocumentJpaEntity getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(SubscriptionBillingDocumentJpaEntity sourceDocument) {
        this.sourceDocument = sourceDocument;
        this.sourceDocumentId = sourceDocument == null ? null : sourceDocument.getId();
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = appliedAmount;
    }

    public BillingDocumentApplicationJpaEntity getReversalOf() {
        return reversalOf;
    }

    public void setReversalOf(BillingDocumentApplicationJpaEntity reversalOf) {
        this.reversalOf = reversalOf;
        this.reversalOfId = reversalOf == null ? null : reversalOf.getId();
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
