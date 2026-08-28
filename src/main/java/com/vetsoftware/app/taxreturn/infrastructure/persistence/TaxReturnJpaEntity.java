package com.vetsoftware.app.taxreturn.infrastructure.persistence;

import com.vetsoftware.app.taxreturn.domain.TaxKind;
import com.vetsoftware.app.taxreturn.domain.TaxReturnStatus;
import com.vetsoftware.app.taxreturn.domain.VatFrequency;
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
 * {@code tax_returns} (changeset 351) — lo que se declaro, y hasta cuando
 * pueden revisarlo.
 *
 * <p>
 * <strong>Esta clase NO alcanza {@code CompanyJpaEntity} por ninguna
 * asociacion.</strong> Son declaraciones de VetSoftware: la tabla no tiene
 * {@code company_id}, y el dia que alguien le cuelgue un {@code @ManyToOne} a
 * companies las cuatro reglas duras de aislamiento de BE-COV se activan sobre
 * la feature entera y rompen el build.
 *
 * <p>
 * <strong>Las tres claves foraneas van como escalares</strong>
 * ({@code municipality_code} contra {@code cities.dane_code},
 * {@code corrects_return_id} contra si misma y {@code filed_by_system_user_id}
 * contra {@code system_users}). Un {@code @ManyToOne} traeria el grafo de
 * geografia entero para usar cinco caracteres, y una autorreferencia mapeada
 * obligaria a un {@code @EntityGraph} en cada finder para no pagar un N+1
 * recorriendo la cadena de correcciones.
 *
 * <p>
 * <strong>Las TRES columnas GENERATED STORED no se mapean, a
 * proposito.</strong> {@code municipality_key} (centinela {@code '-'} para las
 * nacionales), {@code vat_frequency_year} (el año solo cuando el impuesto es
 * IVA, que es lo que hace que {@code fk_tax_returns_vat_frequency} no se
 * compruebe en los otros tres) y {@code current_return_marker} (una sola
 * declaracion vigente por periodo) las calcula MySQL. Mapearlas invitaria a
 * escribirlas desde Java, y el primer {@code INSERT} que llevara un valor
 * propio para una columna generada lo rechazaria el motor con el error 3105.
 *
 * <p>
 * <strong>Sin {@code enabled} y con {@code @Version}</strong>: es un documento
 * probatorio —deshabilitarlo lo sacaria de los informes sin rastro— pero
 * <em>si</em> se reescribe, porque «se resuelve»: se presenta, se corrige o se
 * anula. Dos operadores que la resuelvan a la vez no se pisan gracias a esa
 * columna.
 */
@Entity
@Table(name = "tax_returns")
public class TaxReturnJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_kind", nullable = false, length = 20)
    private TaxKind taxKind;

    /**
     * <strong>Es {@code short} y no {@code int}.</strong> La columna es
     * {@code SMALLINT}; Hibernate mapea {@code short} a {@code Types.SMALLINT} e
     * {@code int} a {@code Types.INTEGER}, y con {@code ddl-auto: validate} ese
     * desajuste impide construir el {@code SessionFactory} — con el, ningun
     * contexto del repositorio arranca. Mismo criterio que
     * {@code DocumentWithholdingJpaEntity} y {@code UvtValueJpaEntity}. El dominio
     * lo expone como {@code int} porque su rango 2020..2100 cabe de sobra.
     */
    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Column(name = "fiscal_period_key", nullable = false, length = 10)
    private String fiscalPeriodKey;

    /** {@code INT} en el esquema, {@code int} aqui. */
    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    /** Nulo salvo en {@code ICA}. La otra mitad la cuida el CHECK del esquema. */
    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    /** Nulo salvo en {@code VAT}. Copiado de {@code vat_filing_periods}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_frequency", length = 15)
    private VatFrequency vatFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaxReturnStatus status;

    @Column(name = "filed_at")
    private LocalDateTime filedAt;

    @Column(name = "filed_by_system_user_id")
    private Long filedBySystemUserId;

    @Column(name = "receipt_ref", length = 100)
    private String receiptRef;

    @Column(name = "file_ref", length = 255)
    private String fileRef;

    @Column(name = "total_generated", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalGenerated;

    @Column(name = "total_deductible", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDeductible;

    @Column(name = "balance_payable", nullable = false, precision = 19, scale = 2)
    private BigDecimal balancePayable;

    @Column(name = "balance_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceCredit;

    /**
     * Hasta cuando pueden revisarla. Es la columna de la que cuelga toda la
     * politica de conservacion de soportes, y por eso tiene su propio indice
     * ({@code ix_tax_returns_firmeza}).
     */
    @Column(name = "firmeza_until")
    private LocalDate firmezaUntil;

    @Column(name = "corrects_return_id")
    private Long correctsReturnId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected TaxReturnJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TaxKind getTaxKind() {
        return taxKind;
    }

    public void setTaxKind(TaxKind taxKind) {
        this.taxKind = taxKind;
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

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(String municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    public VatFrequency getVatFrequency() {
        return vatFrequency;
    }

    public void setVatFrequency(VatFrequency vatFrequency) {
        this.vatFrequency = vatFrequency;
    }

    public TaxReturnStatus getStatus() {
        return status;
    }

    public void setStatus(TaxReturnStatus status) {
        this.status = status;
    }

    public LocalDateTime getFiledAt() {
        return filedAt;
    }

    public void setFiledAt(LocalDateTime filedAt) {
        this.filedAt = filedAt;
    }

    public Long getFiledBySystemUserId() {
        return filedBySystemUserId;
    }

    public void setFiledBySystemUserId(Long filedBySystemUserId) {
        this.filedBySystemUserId = filedBySystemUserId;
    }

    public String getReceiptRef() {
        return receiptRef;
    }

    public void setReceiptRef(String receiptRef) {
        this.receiptRef = receiptRef;
    }

    public String getFileRef() {
        return fileRef;
    }

    public void setFileRef(String fileRef) {
        this.fileRef = fileRef;
    }

    public BigDecimal getTotalGenerated() {
        return totalGenerated;
    }

    public void setTotalGenerated(BigDecimal totalGenerated) {
        this.totalGenerated = totalGenerated;
    }

    public BigDecimal getTotalDeductible() {
        return totalDeductible;
    }

    public void setTotalDeductible(BigDecimal totalDeductible) {
        this.totalDeductible = totalDeductible;
    }

    public BigDecimal getBalancePayable() {
        return balancePayable;
    }

    public void setBalancePayable(BigDecimal balancePayable) {
        this.balancePayable = balancePayable;
    }

    public BigDecimal getBalanceCredit() {
        return balanceCredit;
    }

    public void setBalanceCredit(BigDecimal balanceCredit) {
        this.balanceCredit = balanceCredit;
    }

    public LocalDate getFirmezaUntil() {
        return firmezaUntil;
    }

    public void setFirmezaUntil(LocalDate firmezaUntil) {
        this.firmezaUntil = firmezaUntil;
    }

    public Long getCorrectsReturnId() {
        return correctsReturnId;
    }

    public void setCorrectsReturnId(Long correctsReturnId) {
        this.correctsReturnId = correctsReturnId;
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
