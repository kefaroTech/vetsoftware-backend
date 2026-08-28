package com.vetsoftware.app.withholdingcertificate.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * El papel que hace descontable una retencion.
 *
 * <p>
 * La retencion que un cliente te practico solo se puede imputar en la
 * declaracion si tienes el certificado. El cliente esta obligado a expedirlo,
 * normalmente una vez al ano y cubriendo <em>todas</em> las retenciones del
 * periodo -por eso un certificado cubre muchas retenciones y no una-. Sin esta
 * ficha, cada ano se pierde lo que nadie reclamo.
 *
 * <p>
 * <strong>La fila nace incompleta a proposito.</strong> Se crea cuando se abre
 * la expectativa del certificado -se sabe quien debe expedirlo, por cuanto y
 * hasta cuando- y se cierra cuando llega. Por eso {@code receivedOn} y
 * {@code fileRef} son nulables, y por eso hay una segunda escritura
 * ({@link #receive}), que es lo que justifica el {@code version} de la tabla.
 *
 * <p>
 * <strong>{@code ratePercent} es un PORCENTAJE, no una fraccion, y por eso
 * lleva seis decimales.</strong> Las tarifas de industria y comercio se
 * expresan <em>por mil</em>: 6,9 por mil es 0,69 %. Con menos precision, 4,14
 * por mil se corta y base por tarifa deja de dar el importe certificado, en
 * silencio y por debajo. La unidad va en el nombre para que nadie la adivine.
 *
 * <p>
 * <strong>{@code legalDeadlineOn} se guarda como dato y no se calcula.</strong>
 * Es el ultimo dia habil de marzo -la unica fecha dura de todo el bloque
 * fiscal-, y calcularla necesita el calendario de festivos, que es de otra
 * capa. Se escribe al crear la fila para poder LISTAR los que faltan por
 * recibir <em>antes</em> de que sea tarde; derivarla al vuelo convertiria ese
 * listado en un barrido de la tabla entera.
 */
public class WithholdingCertificate {

    private static final int MIN_FISCAL_YEAR = 2020;
    private static final int MAX_FISCAL_YEAR = 2100;
    private static final int MAX_TAX_ID_LENGTH = 50;
    private static final int MAX_CERTIFICATE_NUMBER_LENGTH = 50;
    private static final int MAX_FILE_REF_LENGTH = 255;
    private static final int MAX_RATE_SCALE = 6;
    private static final BigDecimal MAX_RATE_PERCENT = new BigDecimal("100");

    /** Espejo del REGEXP de {@code chk_withholding_certificates_period}. */
    private static final Pattern BIMONTHLY_PERIOD = Pattern.compile("^\\d{4}-B0[1-6]$");

    private static final String ANNUAL_PERIOD_SUFFIX = "-A";

    private final Long id;
    private final Long companyId;

    /** NIT del cliente que practico la retencion y debe expedir el papel. */
    private final String issuedByTaxId;

    private final String certificateNumber;
    private final WithholdingType withholdingType;

    /** Ano gravable. Sin el, la retencion no se imputa a ninguna declaracion. */
    private final Integer fiscalYear;

    private final String fiscalPeriodKey;
    private final BigDecimal ratePercent;
    private final BigDecimal certifiedAmount;
    private final LocalDate issuedOn;
    private final LocalDate legalDeadlineOn;
    private final LocalDateTime createdDate;

    /** Cuando llego el certificado. Nulo mientras la expectativa sigue abierta. */
    private LocalDate receivedOn;

    private String fileRef;
    private SubstituteEvidenceKind substituteEvidenceKind;
    private String substituteEvidenceRef;

    public WithholdingCertificate(Long id, Long companyId, String issuedByTaxId,
            String certificateNumber, WithholdingType withholdingType, Integer fiscalYear,
            String fiscalPeriodKey, BigDecimal ratePercent, BigDecimal certifiedAmount,
            LocalDate issuedOn, LocalDate legalDeadlineOn, LocalDate receivedOn, String fileRef,
            SubstituteEvidenceKind substituteEvidenceKind, String substituteEvidenceRef,
            LocalDateTime createdDate) {
        validate(companyId, issuedByTaxId, certificateNumber, withholdingType, fiscalYear,
                fiscalPeriodKey, ratePercent, certifiedAmount, issuedOn, legalDeadlineOn,
                receivedOn, fileRef, substituteEvidenceKind, substituteEvidenceRef);
        this.id = id;
        this.companyId = companyId;
        this.issuedByTaxId = issuedByTaxId;
        this.certificateNumber = certificateNumber;
        this.withholdingType = withholdingType;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriodKey = fiscalPeriodKey;
        this.ratePercent = ratePercent;
        this.certifiedAmount = certifiedAmount;
        this.issuedOn = issuedOn;
        this.legalDeadlineOn = legalDeadlineOn;
        this.receivedOn = receivedOn;
        this.fileRef = fileRef;
        this.substituteEvidenceKind = substituteEvidenceKind;
        this.substituteEvidenceRef = substituteEvidenceRef;
        this.createdDate = createdDate;
    }

    /**
     * Abre la expectativa de un certificado: se sabe quien debe expedirlo, por
     * cuanto y hasta cuando, y todavia no ha llegado.
     *
     * <p>
     * Nace sin {@code receivedOn}, sin {@code fileRef} y sin sustituto, que son los
     * tres hechos que aun no han ocurrido. Cerrarlos es la segunda escritura.
     */
    public static WithholdingCertificate register(Long companyId, String issuedByTaxId,
            String certificateNumber, WithholdingType withholdingType, Integer fiscalYear,
            String fiscalPeriodKey, BigDecimal ratePercent, BigDecimal certifiedAmount,
            LocalDate issuedOn, LocalDate legalDeadlineOn, LocalDateTime createdDate) {
        return new WithholdingCertificate(null, companyId, issuedByTaxId, certificateNumber,
                withholdingType, fiscalYear, fiscalPeriodKey, ratePercent, certifiedAmount,
                issuedOn, legalDeadlineOn, null, null, null, null, createdDate);
    }

    /**
     * El certificado llego: se cierra la expectativa con su fecha y su archivo.
     *
     * <p>
     * <strong>Solo una vez.</strong> Un segundo {@code receive} pisaria
     * {@code fileRef} y dejaria el expediente sin el soporte que ya se habia
     * guardado -y el motor lo aceptaria sin queja, porque es un {@code UPDATE}
     * perfectamente valido-. Es la unica regla de este agregado que la base no
     * cuida, y por eso es la unica que lanza una excepcion propia.
     *
     * <p>
     * Al llegar el certificado, el sustituto deja de tener sentido y se retira:
     * {@code chk_withholding_certificates_substitute} prohibe que convivan, asi que
     * conservarlo haria fallar el {@code UPDATE} como un error de integridad en vez
     * de como lo que es, un hecho que ya no aplica.
     */
    public void receive(LocalDate receivedOn, String fileRef) {
        if (this.receivedOn != null)
            throw new WithholdingCertificateAlreadyReceivedException(id, this.receivedOn);
        validateReceipt(receivedOn, fileRef, issuedOn);
        this.receivedOn = receivedOn;
        this.fileRef = fileRef;
        this.substituteEvidenceKind = null;
        this.substituteEvidenceRef = null;
    }

    /**
     * El cliente no expidio el certificado y la retencion se acredita con el
     * comprobante de pago, que es el unico sustituto que la ley admite.
     *
     * <p>
     * <strong>Solo mientras el certificado no ha llegado.</strong> Un sustituto
     * sobre un certificado ya recibido no es un dato de mas: es una segunda prueba
     * del mismo hecho, y la constraint del esquema tampoco lo admite.
     */
    public void attachSubstituteEvidence(SubstituteEvidenceKind kind, String ref) {
        if (receivedOn != null)
            throw new WithholdingCertificateAlreadyReceivedException(id, receivedOn);
        validateSubstitute(kind, ref, null);
        this.substituteEvidenceKind = kind;
        this.substituteEvidenceRef = ref;
    }

    /** Sigue faltando el papel. Es el criterio del barrido de vencimientos. */
    public boolean isMissing() {
        return receivedOn == null;
    }

    /** Lo que hoy se puede imputar: el papel, o el sustituto que lo suple. */
    public boolean isSupported() {
        return receivedOn != null || substituteEvidenceKind != null;
    }

    private static void validate(Long companyId, String issuedByTaxId, String certificateNumber,
            WithholdingType withholdingType, Integer fiscalYear, String fiscalPeriodKey,
            BigDecimal ratePercent, BigDecimal certifiedAmount, LocalDate issuedOn,
            LocalDate legalDeadlineOn, LocalDate receivedOn, String fileRef,
            SubstituteEvidenceKind substituteEvidenceKind, String substituteEvidenceRef) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (issuedByTaxId == null || issuedByTaxId.isBlank())
            throw new IllegalArgumentException("issuedByTaxId is required");
        if (issuedByTaxId.length() > MAX_TAX_ID_LENGTH)
            throw new IllegalArgumentException("issuedByTaxId must be 50 chars or less");
        if (certificateNumber == null || certificateNumber.isBlank())
            throw new IllegalArgumentException("certificateNumber is required");
        if (certificateNumber.length() > MAX_CERTIFICATE_NUMBER_LENGTH)
            throw new IllegalArgumentException("certificateNumber must be 50 chars or less");
        if (withholdingType == null)
            throw new IllegalArgumentException("withholdingType is required");
        validateFiscalYear(fiscalYear);
        validateFiscalPeriodKey(withholdingType, fiscalYear, fiscalPeriodKey);
        validateRatePercent(ratePercent);
        // Espejo de chk_withholding_certificates_amount.
        if (certifiedAmount == null)
            throw new IllegalArgumentException("certifiedAmount is required");
        if (certifiedAmount.signum() <= 0)
            throw new IllegalArgumentException("certifiedAmount must be greater than zero");
        if (issuedOn == null)
            throw new IllegalArgumentException("issuedOn is required");
        // El vencimiento legal se guarda, no se calcula: sin el no hay forma de
        // listar los que faltan por recibir antes de que sea tarde.
        if (legalDeadlineOn == null)
            throw new IllegalArgumentException("legalDeadlineOn is required");
        validateReceiptOrAbsence(receivedOn, fileRef, issuedOn);
        validateSubstitute(substituteEvidenceKind, substituteEvidenceRef, receivedOn);
    }

    /**
     * Espejo de {@code chk_withholding_certificates_year}. Sin ano gravable la
     * retencion no se puede imputar a ninguna declaracion: perderlo es perder el
     * derecho a descontarla y acabar pagando dos veces el mismo impuesto.
     */
    private static void validateFiscalYear(Integer fiscalYear) {
        if (fiscalYear == null)
            throw new IllegalArgumentException("fiscalYear is required");
        if (fiscalYear < MIN_FISCAL_YEAR || fiscalYear > MAX_FISCAL_YEAR)
            throw new IllegalArgumentException("fiscalYear must be between 2020 and 2100");
    }

    /**
     * Espejo exacto de {@code chk_withholding_certificates_period}, con sus tres
     * mitades: la forma depende del impuesto -{@code YYYY-A} para renta, que se
     * certifica por ano, y de {@code YYYY-B01} a {@code YYYY-B06} para IVA e ICA,
     * que se certifican por bimestre- y en los dos casos el ano de la clave tiene
     * que ser el mismo {@code fiscalYear} de la fila.
     *
     * <p>
     * Esa tercera comprobacion es la que parece redundante y no lo es: un
     * {@code 2025-B03} colgado de un {@code fiscal_year} de 2026 se lee bien, pasa
     * el formato, y manda la retencion a la declaracion equivocada.
     */
    private static void validateFiscalPeriodKey(WithholdingType withholdingType, Integer fiscalYear,
            String fiscalPeriodKey) {
        if (fiscalPeriodKey == null || fiscalPeriodKey.isBlank())
            throw new IllegalArgumentException("fiscalPeriodKey is required");
        if (withholdingType == WithholdingType.INCOME_TAX) {
            if (!fiscalPeriodKey.equals(fiscalYear + ANNUAL_PERIOD_SUFFIX))
                throw new IllegalArgumentException("fiscalPeriodKey must be " + fiscalYear
                        + ANNUAL_PERIOD_SUFFIX + " for INCOME_TAX, but was " + fiscalPeriodKey);
            return;
        }
        if (!BIMONTHLY_PERIOD.matcher(fiscalPeriodKey).matches())
            throw new IllegalArgumentException("fiscalPeriodKey must match YYYY-B01..YYYY-B06 for "
                    + withholdingType + ", but was " + fiscalPeriodKey);
        if (!fiscalPeriodKey.startsWith(String.valueOf(fiscalYear)))
            throw new IllegalArgumentException("fiscalPeriodKey year must match fiscalYear "
                    + fiscalYear + ", but was " + fiscalPeriodKey);
    }

    /**
     * Espejo de {@code chk_withholding_certificates_rate}, mas la escala.
     *
     * <p>
     * La escala no esta en la constraint porque la columna ya es
     * {@code DECIMAL(9,6)} y MySQL <em>redondea en silencio</em> lo que le sobra:
     * un 0,6912345 entra como 0,691235 sin un aviso. Comprobarlo aqui convierte esa
     * perdida en un error visible, que es la diferencia entre corregir el dato y
     * descubrir el descuadre cuando ya no se puede.
     */
    private static void validateRatePercent(BigDecimal ratePercent) {
        if (ratePercent == null)
            throw new IllegalArgumentException("ratePercent is required");
        if (ratePercent.signum() <= 0)
            throw new IllegalArgumentException("ratePercent must be greater than zero");
        if (ratePercent.compareTo(MAX_RATE_PERCENT) > 0)
            throw new IllegalArgumentException("ratePercent must be 100 or less");
        if (ratePercent.stripTrailingZeros().scale() > MAX_RATE_SCALE)
            throw new IllegalArgumentException("ratePercent must have 6 decimals or less");
    }

    /**
     * Espejo de {@code chk_withholding_certificates_dates} y de
     * {@code chk_withholding_certificates_file}, en su forma completa: o el papel
     * no ha llegado, o llego con su fecha y su archivo.
     */
    private static void validateReceiptOrAbsence(LocalDate receivedOn, String fileRef,
            LocalDate issuedOn) {
        if (receivedOn == null) {
            if (fileRef != null && fileRef.length() > MAX_FILE_REF_LENGTH)
                throw new IllegalArgumentException("fileRef must be 255 chars or less");
            return;
        }
        validateReceipt(receivedOn, fileRef, issuedOn);
    }

    private static void validateReceipt(LocalDate receivedOn, String fileRef, LocalDate issuedOn) {
        if (receivedOn == null)
            throw new IllegalArgumentException("receivedOn is required");
        if (receivedOn.isBefore(issuedOn))
            throw new IllegalArgumentException("receivedOn cannot be before issuedOn");
        // Un certificado recibido sin archivo es un certificado que nadie puede
        // ensenar: la fecha sola no prueba nada ante la administracion.
        if (fileRef == null || fileRef.isBlank())
            throw new IllegalArgumentException("fileRef is required once the certificate arrives");
        if (fileRef.length() > MAX_FILE_REF_LENGTH)
            throw new IllegalArgumentException("fileRef must be 255 chars or less");
    }

    /**
     * Espejo de {@code chk_withholding_certificates_substitute}: o los dos campos
     * nulos, o el comprobante de pago completo y con el certificado todavia sin
     * llegar. El sustituto solo tiene sentido mientras el papel no esta.
     */
    private static void validateSubstitute(SubstituteEvidenceKind kind, String ref,
            LocalDate receivedOn) {
        if (kind == null && ref == null)
            return;
        if (kind == null || ref == null || ref.isBlank())
            throw new IllegalArgumentException(
                    "substitute evidence needs both its kind and its reference");
        if (ref.length() > MAX_FILE_REF_LENGTH)
            throw new IllegalArgumentException("substituteEvidenceRef must be 255 chars or less");
        if (receivedOn != null)
            throw new IllegalArgumentException(
                    "substitute evidence is not allowed once the certificate arrived");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getIssuedByTaxId() {
        return issuedByTaxId;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public WithholdingType getWithholdingType() {
        return withholdingType;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public String getFiscalPeriodKey() {
        return fiscalPeriodKey;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public BigDecimal getCertifiedAmount() {
        return certifiedAmount;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getLegalDeadlineOn() {
        return legalDeadlineOn;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public String getFileRef() {
        return fileRef;
    }

    public SubstituteEvidenceKind getSubstituteEvidenceKind() {
        return substituteEvidenceKind;
    }

    public String getSubstituteEvidenceRef() {
        return substituteEvidenceRef;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
