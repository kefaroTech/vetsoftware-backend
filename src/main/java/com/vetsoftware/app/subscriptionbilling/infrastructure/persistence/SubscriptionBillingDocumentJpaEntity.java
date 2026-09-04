package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * La cuenta de cobro que Lumbre calcula y numera.
 *
 * <p>
 * <b>Lleva {@code @Version}</b> (BE-26): es la cabecera del agregado, y sobre
 * ella se apoyan las exenciones de las líneas de impuesto
 * ({@code E1_APPEND_ONLY}) y de los cargos ({@code E6_YA_PROTEGIDO}).
 *
 * <p>
 * <b>Sin {@code @SQLDelete} y sin {@code @SQLRestriction}</b>: la tabla no
 * lleva {@code enabled}. Una cuenta de cobro no se borra — se anula
 * ({@code issue_status = 'VOIDED'}) o se corrige con una nota crédito
 * encadenada—. Ojo con la consecuencia: como no hay {@code @SQLDelete}, la
 * trampa de {@code BORRADO_LOGICO_RESPETA_LA_VERSION} —Hibernate liga
 * {@code id} y {@code version} al SQL de borrado de una entidad versionada— no
 * aplica aquí. Si algún día alguien le añade borrado lógico a esta tabla, el
 * {@code @SQLDelete} tendrá que llevar {@code AND version = ?}.
 *
 * <p>
 * <b>Las tres columnas generadas no se escriben nunca, y dos ni siquiera se
 * mapean.</b>
 * <ul>
 * <li>{@code balance_amount} ({@code VIRTUAL}) se mapea de solo lectura con
 * {@code @Generated}: es la columna que decide si una cuenta entra en mora, así
 * que <b>un camino de código capaz de desincronizarla es un camino capaz de
 * suspender a quien ya pagó</b>. No tiene <em>setter</em> — no es un descuido,
 * es la barrera.</li>
 * <li>{@code recurring_cycle_marker} y {@code overdue_marker} ({@code STORED})
 * <b>no se mapean en absoluto</b>. No hacen falta para nada que el código
 * necesite leer y, sin mapeo, no existe ni la posibilidad teórica de
 * escribirlas. Sus consultas van por SQL nativo contra el índice.</li>
 * </ul>
 *
 * <p>
 * Las referencias son ids pelados por el mismo motivo que en los cargos: las FK
 * son <b>compuestas</b> y arrastran la empresa ({@code fk_sbd_subscription},
 * {@code fk_sbd_corrects}); mapearlas como asociación de una sola columna
 * deshace esa garantía.
 */
@Entity
@Table(name = "subscription_billing_documents")
public class SubscriptionBillingDocumentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_number", nullable = false, length = 30)
    private String documentNumber;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_kind", nullable = false, length = 20)
    private DocumentKind documentKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_reason", nullable = false, length = 20)
    private BillingReason billingReason;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false, length = 20)
    private IssueStatus issueStatus;

    @Column(name = "external_invoice_number", length = 60)
    private String externalInvoiceNumber;

    @Column(name = "external_cufe", length = 100)
    private String externalCufe;

    /** <b>La fecha fiscal</b>, y la única desde la que se cuenta el vencimiento. */
    @Column(name = "external_issued_at")
    private LocalDate externalIssuedAt;

    @Column(name = "external_provider", length = 40)
    private String externalProvider;

    @Column(name = "external_registered_at")
    private LocalDateTime externalRegisteredAt;

    @Column(name = "external_registered_by_system_user_id")
    private Long externalRegisteredBySystemUserId;

    @Column(name = "corrects_document_id")
    private Long correctsDocumentId;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "settled_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal settledAmount;

    /**
     * Columna calculada {@code VIRTUAL} de la base:
     * {@code total_amount - settled_amount}.
     *
     * <p>
     * <b>Solo lectura, y sin mutador.</b> {@code insertable = false} y
     * {@code updatable = false} la sacan del {@code INSERT} y del {@code UPDATE}
     * —sin eso MySQL rechaza la sentencia entera, porque una columna generada no
     * admite valor—, y {@code @Generated} le dice a Hibernate que la vuelva a leer
     * después de cada escritura.
     */
    @Column(name = "balance_amount", insertable = false, updatable = false, precision = 19, scale = 2)
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    private BigDecimal balanceAmount;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SubscriptionBillingDocumentJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public DocumentKind getDocumentKind() {
        return documentKind;
    }

    public void setDocumentKind(DocumentKind documentKind) {
        this.documentKind = documentKind;
    }

    public BillingReason getBillingReason() {
        return billingReason;
    }

    public void setBillingReason(BillingReason billingReason) {
        this.billingReason = billingReason;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public IssueStatus getIssueStatus() {
        return issueStatus;
    }

    public void setIssueStatus(IssueStatus issueStatus) {
        this.issueStatus = issueStatus;
    }

    public String getExternalInvoiceNumber() {
        return externalInvoiceNumber;
    }

    public void setExternalInvoiceNumber(String externalInvoiceNumber) {
        this.externalInvoiceNumber = externalInvoiceNumber;
    }

    public String getExternalCufe() {
        return externalCufe;
    }

    public void setExternalCufe(String externalCufe) {
        this.externalCufe = externalCufe;
    }

    public LocalDate getExternalIssuedAt() {
        return externalIssuedAt;
    }

    public void setExternalIssuedAt(LocalDate externalIssuedAt) {
        this.externalIssuedAt = externalIssuedAt;
    }

    public String getExternalProvider() {
        return externalProvider;
    }

    public void setExternalProvider(String externalProvider) {
        this.externalProvider = externalProvider;
    }

    public LocalDateTime getExternalRegisteredAt() {
        return externalRegisteredAt;
    }

    public void setExternalRegisteredAt(LocalDateTime externalRegisteredAt) {
        this.externalRegisteredAt = externalRegisteredAt;
    }

    public Long getExternalRegisteredBySystemUserId() {
        return externalRegisteredBySystemUserId;
    }

    public void setExternalRegisteredBySystemUserId(Long externalRegisteredBySystemUserId) {
        this.externalRegisteredBySystemUserId = externalRegisteredBySystemUserId;
    }

    public Long getCorrectsDocumentId() {
        return correctsDocumentId;
    }

    public void setCorrectsDocumentId(Long correctsDocumentId) {
        this.correctsDocumentId = correctsDocumentId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(BigDecimal settledAmount) {
        this.settledAmount = settledAmount;
    }

    /**
     * El saldo que calculó la base. <b>No hay {@code setBalanceAmount} y su
     * ausencia es la garantía</b>: ningún mapper, ningún servicio y ningún test
     * pueden escribirlo.
     */
    public BigDecimal getBalanceAmount() {
        return balanceAmount;
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
