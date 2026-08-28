package com.vetsoftware.app.withholdingcertificate.infrastructure.persistence;

import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import com.vetsoftware.app.withholdingcertificate.domain.WithholdingType;
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
 * {@code withholding_certificates} - el papel que hace descontable la
 * retencion.
 *
 * <p>
 * <strong>{@code company_id} va como escalar {@code Long} y NO como
 * {@code @ManyToOne} a {@code CompanyJpaEntity}</strong>, y no es descuido. La
 * tabla declara ademas la unicidad {@code (company_id, id)}, que existe para
 * servir de destino a la clave foranea <em>compuesta</em> de
 * {@code document_withholdings}. Mapear una FK compuesta como asociacion obliga
 * a un {@code @JoinColumns} que comparte la columna {@code company_id} con los
 * demas mapeos; Hibernate exige que todas las columnas de una propiedad tengan
 * el mismo modo de escritura y solo un mapeo puede ser dueno de una columna
 * fisica, asi que las asociaciones tendrian que ir todas
 * {@code insertable = false, updatable = false}. Es la trampa que documenta
 * {@code PaymentRefundJpaEntity}, y ahi el fallo ni siquiera senala a la clase
 * culpable: revienta el {@code entityManagerFactory} y se lleva por delante la
 * aplicacion entera.
 *
 * <p>
 * Sin asociaciones tampoco hay N+1 que evitar ni {@code @EntityGraph} que
 * poner. La FK sigue existiendo y sigue vigilando en la base; lo que no existe
 * es la navegacion desde Java.
 *
 * <p>
 * <strong>Con {@code @Version}, y la tabla trae la columna.</strong> Esta ficha
 * <em>si</em> se edita: nace como expectativa y se cierra cuando el certificado
 * llega ({@code received_on} + {@code file_ref}), o se acredita con el
 * sustituto. Son dos escrituras separadas en el tiempo sobre la misma fila, que
 * es exactamente el escenario que {@code @Version} protege: sin el, quien
 * adjunta el comprobante de pago y quien registra la llegada del papel se
 * pisan, y el que pierde no se entera -sin excepcion y sin log-.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}.</strong> Un certificado
 * no se oculta: la ausencia del papel es justamente lo que hay que poder
 * listar, y un {@code @SQLRestriction} esconderia la mitad de los que faltan.
 */
@Entity
@Table(name = "withholding_certificates")
public class WithholdingCertificateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "issued_by_tax_id", nullable = false, length = 50)
    private String issuedByTaxId;

    @Column(name = "certificate_number", nullable = false, length = 50)
    private String certificateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "withholding_type", nullable = false, length = 20)
    private WithholdingType withholdingType;

    /**
     * {@code short} y no {@code int}: la columna es {@code SMALLINT} y con
     * {@code ddl-auto: validate} un {@code Integer} se anuncia como
     * {@code Types#INTEGER} y rompe el arranque. Mismo criterio que
     * {@code PlatformAccessRequestJpaEntity.maxAttempts}.
     */
    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Column(name = "fiscal_period_key", nullable = false, length = 10)
    private String fiscalPeriodKey;

    /**
     * PORCENTAJE con seis decimales. Las tarifas de ICA se expresan por mil: con
     * menos precision, base por tarifa deja de dar el importe certificado.
     */
    @Column(name = "rate_percent", nullable = false, precision = 9, scale = 6)
    private BigDecimal ratePercent;

    @Column(name = "certified_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal certifiedAmount;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    /**
     * Ultimo dia habil de marzo. Dato guardado, no formula: ver el changeset 328.
     */
    @Column(name = "legal_deadline_on", nullable = false)
    private LocalDate legalDeadlineOn;

    @Column(name = "received_on")
    private LocalDate receivedOn;

    @Column(name = "file_ref", length = 255)
    private String fileRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "substitute_evidence_kind", length = 20)
    private SubstituteEvidenceKind substituteEvidenceKind;

    @Column(name = "substitute_evidence_ref", length = 255)
    private String substituteEvidenceRef;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected WithholdingCertificateJpaEntity() {
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

    public String getIssuedByTaxId() {
        return issuedByTaxId;
    }

    public void setIssuedByTaxId(String issuedByTaxId) {
        this.issuedByTaxId = issuedByTaxId;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public WithholdingType getWithholdingType() {
        return withholdingType;
    }

    public void setWithholdingType(WithholdingType withholdingType) {
        this.withholdingType = withholdingType;
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

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public void setRatePercent(BigDecimal ratePercent) {
        this.ratePercent = ratePercent;
    }

    public BigDecimal getCertifiedAmount() {
        return certifiedAmount;
    }

    public void setCertifiedAmount(BigDecimal certifiedAmount) {
        this.certifiedAmount = certifiedAmount;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDate issuedOn) {
        this.issuedOn = issuedOn;
    }

    public LocalDate getLegalDeadlineOn() {
        return legalDeadlineOn;
    }

    public void setLegalDeadlineOn(LocalDate legalDeadlineOn) {
        this.legalDeadlineOn = legalDeadlineOn;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(LocalDate receivedOn) {
        this.receivedOn = receivedOn;
    }

    public String getFileRef() {
        return fileRef;
    }

    public void setFileRef(String fileRef) {
        this.fileRef = fileRef;
    }

    public SubstituteEvidenceKind getSubstituteEvidenceKind() {
        return substituteEvidenceKind;
    }

    public void setSubstituteEvidenceKind(SubstituteEvidenceKind substituteEvidenceKind) {
        this.substituteEvidenceKind = substituteEvidenceKind;
    }

    public String getSubstituteEvidenceRef() {
        return substituteEvidenceRef;
    }

    public void setSubstituteEvidenceRef(String substituteEvidenceRef) {
        this.substituteEvidenceRef = substituteEvidenceRef;
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
