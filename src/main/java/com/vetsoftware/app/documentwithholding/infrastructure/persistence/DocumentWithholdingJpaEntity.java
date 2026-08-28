package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
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
 * {@code document_withholdings} - la retencion que te practico el cliente.
 *
 * <p>
 * <strong>Con {@code @Version} y sin {@code enabled}, y las dos mitades tienen
 * motivo.</strong> Sin borrado logico porque una retencion no se desactiva: se
 * corrige con otra fila, y un {@code @SQLRestriction} escondería la mitad del
 * cruce contra los certificados. Con bloqueo optimista porque, a diferencia de
 * las devoluciones, esta fila <em>si</em> tiene una segunda escritura declarada
 * —apuntar {@code certificate_id} cuando el certificado llega—, y dos operarios
 * certificando a la vez desde la misma bandeja se pisarian sin excepcion y sin
 * log.
 *
 * <p>
 * <strong>Las FK van como escalares y no como {@code @ManyToOne}, y no es
 * pereza.</strong> Las dos referencias fiscales son <em>compuestas</em>
 * —{@code (company_id, billing_document_id)} y
 * {@code (company_id, certificate_id)}— y <strong>comparten la columna
 * {@code company_id}</strong>. Como asociaciones obligarian a dos
 * {@code @JoinColumns} sobre la misma columna fisica, e Hibernate exige que
 * todas las columnas de una propiedad tengan el mismo modo de escritura y que
 * solo un mapeo sea dueno de una columna: habria que poner las dos
 * {@code insertable = false, updatable = false} y la empresa dejaria de
 * escribirse. Es la trampa que documenta {@code PaymentRefundJpaEntity}, y ahi
 * el fallo ni siquiera senala a la clase culpable: revienta el
 * {@code entityManagerFactory} y se lleva por delante la aplicacion entera.
 *
 * <p>
 * Sin asociaciones tampoco hay N+1 que evitar ni {@code @EntityGraph} que
 * poner. Las FK siguen existiendo y siguen vigilando en la base; lo que no
 * existe es la navegacion desde Java.
 *
 * <p>
 * <strong>{@code municipality_key} NO se mapea.</strong> Es una columna
 * {@code GENERATED ALWAYS ... STORED} que solo existe para que
 * {@code uq_document_withholdings_case} pueda distinguir dos retenciones
 * nacionales del mismo documento —en un indice unico dos {@code NULL} no chocan
 * entre si—. Mapearla haria que Hibernate intentara escribirla y MySQL
 * rechazaria el {@code INSERT}: una columna generada no admite valor.
 */
@Entity
@Table(name = "document_withholdings")
public class DocumentWithholdingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "billing_document_id", nullable = false)
    private Long billingDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "withholding_type", nullable = false, length = 20)
    private WithholdingType type;

    @Column(name = "taxable_base", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableBase;

    /**
     * Seis decimales, y son los que la columna declara. Con menos, una tarifa de
     * ICA expresada por mil ({@code 4.14} por mil es {@code 0.414000} por ciento)
     * se redondea al guardarla y base por tarifa deja de dar el importe
     * certificado.
     */
    @Column(name = "rate_percent", nullable = false, precision = 9, scale = 6)
    private BigDecimal ratePercent;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "municipality_code", length = 5)
    private String municipalityCode;

    /**
     * {@code short} y no {@code int}: la columna es {@code SMALLINT} y un
     * {@code int} lo mapearia Hibernate como {@code integer}, con el consiguiente
     * desajuste de tipo en {@code ddl-auto: validate} —que no falla al escribir,
     * falla al arrancar la aplicacion—. Mismo criterio que
     * {@code platform_access_requests.max_attempts}. El dominio lo expone como
     * {@code int} porque su rango 2020..2100 cabe de sobra y evita conversiones en
     * cada llamada.
     */
    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Column(name = "fiscal_period_key", nullable = false, length = 10)
    private String fiscalPeriodKey;

    @Column(name = "practiced_on", nullable = false)
    private LocalDate practicedOn;

    /** Nulo mientras la retencion no tenga respaldo documental. */
    @Column(name = "certificate_id")
    private Long certificateId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected DocumentWithholdingJpaEntity() {
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

    public WithholdingType getType() {
        return type;
    }

    public void setType(WithholdingType type) {
        this.type = type;
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

    public Long getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
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
