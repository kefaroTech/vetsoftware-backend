package com.vetsoftware.app.supplierwithholding.infrastructure.persistence;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
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
 * {@code supplier_withholdings} (changeset 352) — lo que le retenemos a otros.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion.</strong> La retencion la practica Lumbre y la tabla no tiene
 * {@code company_id}; el dia que alguien le cuelgue un {@code @ManyToOne} a
 * companies, las cuatro reglas duras de aislamiento de BE-COV se activan sobre
 * la feature entera y rompen el build.
 *
 * <p>
 * <strong>La FK al municipio va como escalar</strong>, igual que en
 * {@code WithholdingRateRuleJpaEntity} y por las mismas dos razones: apunta a
 * {@code cities.dane_code} y no a {@code cities.id}, y un {@code @ManyToOne}
 * traeria el grafo de geografia entero para usar cinco caracteres.
 * {@code fk_sw_municipality} sigue vigilando en la base.
 *
 * <p>
 * <strong>{@code municipality_key} no se mapea</strong>: es
 * {@code GENERATED ALWAYS … STORED} y existe para que
 * {@code uq_supplier_withholdings_case} pueda restringir lo que con
 * {@code NULL} no restringia. Escribirla desde Java haria que el motor
 * rechazara el {@code INSERT} con el error 3105.
 *
 * <p>
 * <strong>Sin {@code enabled} y con {@code @Version}</strong>: es un documento
 * probatorio, pero recibe un acuse y un certificado que llegan tarde. Dos
 * operadores que emitan el certificado a la vez no se pisan gracias a esa
 * columna.
 */
@Entity
@Table(name = "supplier_withholdings")
public class SupplierWithholdingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_tax_id", nullable = false, length = 50)
    private String supplierTaxId;

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_doc_type", nullable = false, length = 15)
    private SupplierDocumentKind supplierDocType;

    @Column(name = "supplier_invoice_ref", nullable = false, length = 100)
    private String supplierInvoiceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "withholding_type", nullable = false, length = 20)
    private SupplierWithholdingType withholdingType;

    @Column(name = "concept", nullable = false, length = 60)
    private String concept;

    @Column(name = "taxable_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableBase;

    /**
     * Porcentaje, no fraccion, y con los seis decimales que declara la columna:
     * {@code precision}/{@code scale} tienen que coincidir con {@code DECIMAL(9,6)}
     * o {@code ddl-auto: validate} lo rechaza al arrancar.
     */
    @Column(name = "rate_percent", nullable = false, precision = 9, scale = 6)
    private BigDecimal ratePercent;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Nulo salvo en {@code ICA}. La otra mitad la cuida el CHECK del esquema. */
    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    /**
     * <strong>Es {@code short} y no {@code int}.</strong> La columna es
     * {@code SMALLINT}; Hibernate mapea {@code short} a {@code Types.SMALLINT} e
     * {@code int} a {@code Types.INTEGER}, y con {@code ddl-auto: validate} ese
     * desajuste impide construir el {@code SessionFactory} — y con el, arrancar
     * cualquier contexto del repositorio. Mismo criterio que
     * {@code DocumentWithholdingJpaEntity}.
     */
    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Column(name = "fiscal_period_key", nullable = false, length = 10)
    private String fiscalPeriodKey;

    @Column(name = "practiced_on", nullable = false)
    private LocalDate practicedOn;

    @Column(name = "certificate_issued_at")
    private LocalDateTime certificateIssuedAt;

    @Column(name = "certificate_ref", length = 100)
    private String certificateRef;

    /** La prueba de la consignacion. Obligacion legal de conservacion. */
    @Column(name = "payment_receipt_ref", length = 255)
    private String paymentReceiptRef;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SupplierWithholdingJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSupplierTaxId() {
        return supplierTaxId;
    }

    public void setSupplierTaxId(String supplierTaxId) {
        this.supplierTaxId = supplierTaxId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public SupplierDocumentKind getSupplierDocType() {
        return supplierDocType;
    }

    public void setSupplierDocType(SupplierDocumentKind supplierDocType) {
        this.supplierDocType = supplierDocType;
    }

    public String getSupplierInvoiceRef() {
        return supplierInvoiceRef;
    }

    public void setSupplierInvoiceRef(String supplierInvoiceRef) {
        this.supplierInvoiceRef = supplierInvoiceRef;
    }

    public SupplierWithholdingType getWithholdingType() {
        return withholdingType;
    }

    public void setWithholdingType(SupplierWithholdingType withholdingType) {
        this.withholdingType = withholdingType;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public BigDecimal getTaxableBase() {
        return taxableBase;
    }

    public void setTaxableBase(BigDecimal taxableBase) {
        this.taxableBase = taxableBase;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(BigDecimal ratePercent) {
        this.ratePercent = ratePercent;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(String municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    public short getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(short fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getFiscalPeriodKey() {
        return fiscalPeriodKey;
    }

    public void setFiscalPeriodKey(String fiscalPeriodKey) {
        this.fiscalPeriodKey = fiscalPeriodKey;
    }

    public LocalDate getPracticedOn() {
        return practicedOn;
    }

    public void setPracticedOn(LocalDate practicedOn) {
        this.practicedOn = practicedOn;
    }

    public LocalDateTime getCertificateIssuedAt() {
        return certificateIssuedAt;
    }

    public void setCertificateIssuedAt(LocalDateTime certificateIssuedAt) {
        this.certificateIssuedAt = certificateIssuedAt;
    }

    public String getCertificateRef() {
        return certificateRef;
    }

    public void setCertificateRef(String certificateRef) {
        this.certificateRef = certificateRef;
    }

    public String getPaymentReceiptRef() {
        return paymentReceiptRef;
    }

    public void setPaymentReceiptRef(String paymentReceiptRef) {
        this.paymentReceiptRef = paymentReceiptRef;
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
