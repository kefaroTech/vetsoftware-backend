package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.persistence;

import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
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

/**
 * {@code external_invoice_reconciliations} — lo que se calculo contra lo que se
 * emitio.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: el estado y la resolucion mutan. Una
 * conciliacion se abre en {@code MISSING_EXTERNAL}, despues recibe la factura
 * del tercero y cambia de estado, y mas tarde se resuelve; son tres escrituras
 * sobre la misma fila, y dos operadores que la abran a la vez en la consola se
 * pisarian <em>sin excepcion y sin log</em>. <strong>Sin
 * {@code enabled}</strong>: una conciliacion no se desactiva, se resuelve.
 *
 * <p>
 * <strong>TODAS las FK van como escalares, ninguna como {@code @ManyToOne}, y
 * es una decision con motivo medido.</strong> La FK al documento de cobro es
 * COMPUESTA {@code (company_id, billing_document_id)}: mapearla como asociacion
 * obligaria a un {@code @JoinColumns} que comparte la columna
 * {@code company_id} con cualquier otro mapeo de esa misma columna, y Hibernate
 * exige que todas las columnas de una propiedad tengan el mismo modo de
 * escritura y que solo un mapeo sea dueño de una columna fisica. Es la trampa
 * que documenta {@code PaymentRefundJpaEntity}, y ahi el fallo <em>ni siquiera
 * senala a la clase culpable</em>: revienta el {@code entityManagerFactory} y
 * se lleva por delante la aplicacion entera.
 *
 * <p>
 * {@code company_id} tambien va como escalar {@code Long}, sin
 * {@code @ManyToOne} a {@code CompanyJpaEntity}. Ademas del motivo de arriba,
 * una asociacion viva hasta {@code companies} haria que las cuatro reglas duras
 * de BE-COV empezaran a mirar toda la feature: su discriminador es precisamente
 * «alguna entidad JPA de la feature alcanza {@code CompanyJpaEntity} por
 * asociaciones».
 *
 * <p>
 * Sin asociaciones no hay N+1 que evitar ni {@code @EntityGraph} que poner. Las
 * FK siguen existiendo y vigilando en la base; lo que no existe es la
 * navegacion desde Java.
 */
@Entity
@Table(name = "external_invoice_reconciliations")
public class ExternalInvoiceReconciliationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "billing_document_id", nullable = false)
    private Long billingDocumentId;

    @Column(name = "external_resolution_number", length = 60)
    private String externalResolutionNumber;

    @Column(name = "external_range_from")
    private Integer externalRangeFrom;

    @Column(name = "external_range_to")
    private Integer externalRangeTo;

    @Column(name = "resolution_valid_until")
    private LocalDate resolutionValidUntil;

    @Column(name = "external_invoice_id", length = 60)
    private String externalInvoiceId;

    @Column(name = "external_cufe", length = 100)
    private String externalCufe;

    @Column(name = "computed_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal computedTotal;

    @Column(name = "computed_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal computedTax;

    @Column(name = "external_total", precision = 19, scale = 2)
    private BigDecimal externalTotal;

    @Column(name = "external_tax", precision = 19, scale = 2)
    private BigDecimal externalTax;

    @Column(name = "difference", precision = 19, scale = 2)
    private BigDecimal difference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExternalInvoiceReconciliationStatus status;

    @Column(name = "resolved_by_system_user_id")
    private Long resolvedBySystemUserId;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", length = 255)
    private String resolutionNote;

    /**
     * Periodo contable {@code YYYY-MM}. <strong>Sin clave foranea, y es una
     * carencia declarada y no un olvido</strong>: apunta a
     * {@code accounting_periods}, que es de otra capa y no existe en ningun
     * changeset del arbol. El formato lo cuidan el dominio y
     * {@code chk_eir_resolved}.
     */
    @Column(name = "posting_period", length = 7)
    private String postingPeriod;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ExternalInvoiceReconciliationJpaEntity() {
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

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public void setBillingDocumentId(Long billingDocumentId) {
        this.billingDocumentId = billingDocumentId;
    }

    public String getExternalResolutionNumber() {
        return externalResolutionNumber;
    }

    public void setExternalResolutionNumber(String externalResolutionNumber) {
        this.externalResolutionNumber = externalResolutionNumber;
    }

    public Integer getExternalRangeFrom() {
        return externalRangeFrom;
    }

    public void setExternalRangeFrom(Integer externalRangeFrom) {
        this.externalRangeFrom = externalRangeFrom;
    }

    public Integer getExternalRangeTo() {
        return externalRangeTo;
    }

    public void setExternalRangeTo(Integer externalRangeTo) {
        this.externalRangeTo = externalRangeTo;
    }

    public LocalDate getResolutionValidUntil() {
        return resolutionValidUntil;
    }

    public void setResolutionValidUntil(LocalDate resolutionValidUntil) {
        this.resolutionValidUntil = resolutionValidUntil;
    }

    public String getExternalInvoiceId() {
        return externalInvoiceId;
    }

    public void setExternalInvoiceId(String externalInvoiceId) {
        this.externalInvoiceId = externalInvoiceId;
    }

    public String getExternalCufe() {
        return externalCufe;
    }

    public void setExternalCufe(String externalCufe) {
        this.externalCufe = externalCufe;
    }

    public BigDecimal getComputedTotal() {
        return computedTotal;
    }

    public void setComputedTotal(BigDecimal computedTotal) {
        this.computedTotal = computedTotal;
    }

    public BigDecimal getComputedTax() {
        return computedTax;
    }

    public void setComputedTax(BigDecimal computedTax) {
        this.computedTax = computedTax;
    }

    public BigDecimal getExternalTotal() {
        return externalTotal;
    }

    public void setExternalTotal(BigDecimal externalTotal) {
        this.externalTotal = externalTotal;
    }

    public BigDecimal getExternalTax() {
        return externalTax;
    }

    public void setExternalTax(BigDecimal externalTax) {
        this.externalTax = externalTax;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public void setDifference(BigDecimal difference) {
        this.difference = difference;
    }

    public ExternalInvoiceReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ExternalInvoiceReconciliationStatus status) {
        this.status = status;
    }

    public Long getResolvedBySystemUserId() {
        return resolvedBySystemUserId;
    }

    public void setResolvedBySystemUserId(Long resolvedBySystemUserId) {
        this.resolvedBySystemUserId = resolvedBySystemUserId;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public String getPostingPeriod() {
        return postingPeriod;
    }

    public void setPostingPeriod(String postingPeriod) {
        this.postingPeriod = postingPeriod;
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
