package com.vetsoftware.app.supplierwithholding.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Lo que Lumbre le retiene a un proveedor: la gemela de
 * {@code document_withholdings} en la direccion contraria.
 *
 * <h2>Sin empresa: la retenemos nosotros, no la clinica</h2>
 *
 * <p>
 * {@code supplier_withholdings} no tiene {@code company_id} y
 * {@code SupplierWithholdingJpaEntity} no alcanza {@code CompanyJpaEntity} por
 * ninguna asociacion; si la alcanzara, las cuatro reglas duras de aislamiento
 * de BE-COV caerian sobre la feature entera.
 *
 * <h2>La tarifa es un PORCENTAJE, no una fraccion</h2>
 *
 * <p>
 * El nombre lleva la unidad dentro ({@code ratePercent}), igual que en
 * {@code withholding_rate_rules}. El ICA de Bogota son <b>6,9 por mil</b>, que
 * se escribe {@code 0.690000}. La columna guarda seis decimales por lo mismo:
 * un 4,14 por mil con dos decimales se corta a {@code 0.41} y se retiene casi
 * un uno por ciento de menos, calculado en silencio. Por eso el constructor
 * rechaza una escala mayor que seis en vez de dejar que MySQL redondee
 * callando.
 *
 * <h2>La factura dentro de la unicidad, y por que</h2>
 *
 * <p>
 * {@code uq_supplier_withholdings_case} es
 * {@code (supplier_tax_id, fiscal_period_key, withholding_type,
 * municipality_key, supplier_invoice_ref)}. <strong>La clave que proponia el
 * documento maestro —sin la factura— prohibia dos facturas distintas del mismo
 * proveedor en el mismo mes</strong>, que es el caso normal. Con la referencia
 * del soporte dentro sigue impedido declarar dos veces la misma retencion al
 * mismo proveedor por el mismo documento —lo que duplicaria el reporte anual de
 * terceros y descuadraria la mensual— y dos facturas distintas caben.
 *
 * <p>
 * Por eso {@code supplierInvoiceRef} es obligatoria: sin el soporte no se
 * cuadra contra el gasto, no se sostiene la deduccion y la unicidad seria
 * falsa.
 *
 * <h2>El centinela del municipio</h2>
 *
 * <p>
 * {@code municipalityCode} es obligatorio si y solo si el tipo es
 * {@link SupplierWithholdingType#ICA}. El dominio guarda {@code null} en las
 * nacionales; el centinela vive en la columna generada
 * {@code municipality_key}, que <b>no se mapea</b>. Sin el, dos retenciones
 * nacionales del mismo supuesto no chocarian —en SQL dos {@code NULL} no son
 * iguales— y la unicidad no restringiria nada.
 */
public class SupplierWithholding {

    private static final int MAX_TAX_ID_LENGTH = 50;
    private static final int MAX_SUPPLIER_NAME_LENGTH = 200;
    private static final int MAX_INVOICE_REF_LENGTH = 100;
    private static final int MAX_CONCEPT_LENGTH = 60;
    private static final int MAX_CERTIFICATE_REF_LENGTH = 100;
    private static final int MAX_PAYMENT_RECEIPT_REF_LENGTH = 255;
    private static final int MUNICIPALITY_CODE_LENGTH = 5;

    /** Espejo de {@code chk_sw_year}. */
    private static final int MIN_FISCAL_YEAR = 2020;
    private static final int MAX_FISCAL_YEAR = 2100;

    /** {@code DECIMAL(19,2)}. */
    private static final int MAX_AMOUNT_SCALE = 2;

    /** {@code DECIMAL(9,6)}: una escala mayor la redondearia MySQL sin avisar. */
    private static final int MAX_RATE_SCALE = 6;

    private static final BigDecimal MAX_RATE_PERCENT = new BigDecimal("100");

    /** {@code 2026-M03}: la retencion en la fuente que practicamos es mensual. */
    private static final Pattern MONTHLY = Pattern.compile("^[0-9]{4}-M(0[1-9]|1[0-2])$");

    /** {@code 2026-B03}: reteiva y reteica son bimestrales. */
    private static final Pattern BIMONTHLY = Pattern.compile("^[0-9]{4}-B0[1-6]$");

    /** El mismo centinela que calcula {@code municipality_key} en la base. */
    public static final String NATIONAL_MUNICIPALITY_KEY = "-";

    private final Long id;
    private final String supplierTaxId;
    private final String supplierName;
    private final SupplierDocumentKind supplierDocType;
    private final String supplierInvoiceRef;
    private final SupplierWithholdingType withholdingType;
    private final String concept;
    private final BigDecimal taxableBase;
    private final BigDecimal ratePercent;
    private final BigDecimal amount;
    private final String municipalityCode;
    private final int fiscalYear;
    private final String fiscalPeriodKey;
    private final LocalDate practicedOn;
    private final LocalDateTime certificateIssuedAt;
    private final String certificateRef;
    private final String paymentReceiptRef;
    private final LocalDateTime createdDate;
    private final Long version;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public SupplierWithholding(Long id, String supplierTaxId, String supplierName,
            SupplierDocumentKind supplierDocType, String supplierInvoiceRef,
            SupplierWithholdingType withholdingType, String concept, BigDecimal taxableBase,
            BigDecimal ratePercent, BigDecimal amount, String municipalityCode, int fiscalYear,
            String fiscalPeriodKey, LocalDate practicedOn, LocalDateTime certificateIssuedAt,
            String certificateRef, String paymentReceiptRef, LocalDateTime createdDate,
            Long version) {
        validate(supplierTaxId, supplierName, supplierDocType, supplierInvoiceRef, withholdingType,
                concept, taxableBase, ratePercent, amount, municipalityCode, fiscalYear,
                fiscalPeriodKey, practicedOn, certificateIssuedAt, certificateRef,
                paymentReceiptRef, createdDate);
        this.id = id;
        this.supplierTaxId = supplierTaxId;
        this.supplierName = supplierName;
        this.supplierDocType = supplierDocType;
        this.supplierInvoiceRef = supplierInvoiceRef;
        this.withholdingType = withholdingType;
        this.concept = concept;
        this.taxableBase = taxableBase;
        this.ratePercent = ratePercent;
        this.amount = amount;
        this.municipalityCode = municipalityCode;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriodKey = fiscalPeriodKey;
        this.practicedOn = practicedOn;
        this.certificateIssuedAt = certificateIssuedAt;
        this.certificateRef = certificateRef;
        this.paymentReceiptRef = paymentReceiptRef;
        this.createdDate = createdDate;
        this.version = version;
    }

    /** Retencion recien practicada: sin certificado y sin acuse de pago todavia. */
    public static SupplierWithholding practice(String supplierTaxId, String supplierName,
            SupplierDocumentKind supplierDocType, String supplierInvoiceRef,
            SupplierWithholdingType withholdingType, String concept, BigDecimal taxableBase,
            BigDecimal ratePercent, BigDecimal amount, String municipalityCode, int fiscalYear,
            String fiscalPeriodKey, LocalDate practicedOn, LocalDateTime createdDate) {
        return new SupplierWithholding(null, supplierTaxId, supplierName, supplierDocType,
                supplierInvoiceRef, withholdingType, concept, taxableBase, ratePercent, amount,
                municipalityCode, fiscalYear, fiscalPeriodKey, practicedOn, null, null, null,
                createdDate, null);
    }

    /**
     * Emite el certificado que hay que entregarle al proveedor.
     *
     * <p>
     * <strong>Se niega si ya estaba emitido</strong>, y esa negativa es toda la
     * barandilla que hay: {@code chk_sw_certificate} solo exige que la fecha y la
     * referencia vayan juntas, no que no se reescriban. El numero del certificado
     * es el que el proveedor usa para descontarse la retencion en su declaracion;
     * cambiarlo despues deja dos documentos incompatibles en circulacion.
     */
    public SupplierWithholding issueCertificate(LocalDateTime issuedAt, String reference) {
        if (certificateIssuedAt != null)
            throw new SupplierWithholdingCertificateAlreadyIssuedException(id, certificateIssuedAt);
        if (issuedAt == null)
            throw new IllegalArgumentException("certificateIssuedAt is required");
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("certificateRef is required");
        return new SupplierWithholding(id, supplierTaxId, supplierName, supplierDocType,
                supplierInvoiceRef, withholdingType, concept, taxableBase, ratePercent, amount,
                municipalityCode, fiscalYear, fiscalPeriodKey, practicedOn, issuedAt, reference,
                paymentReceiptRef, createdDate, version);
    }

    /**
     * Anota la prueba de la consignacion a la DIAN o al municipio.
     *
     * <p>
     * Es un documento que <b>llega tarde</b> —despues de practicar la retencion y a
     * veces despues del certificado— y por eso la fila se reescribe y lleva
     * {@code @Version}. Conservar el recibo de pago es obligacion expresa del art.
     * 632 ET: sin el no se puede probar que lo retenido se consigno.
     */
    public SupplierWithholding registerPaymentReceipt(String reference) {
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("paymentReceiptRef is required");
        return new SupplierWithholding(id, supplierTaxId, supplierName, supplierDocType,
                supplierInvoiceRef, withholdingType, concept, taxableBase, ratePercent, amount,
                municipalityCode, fiscalYear, fiscalPeriodKey, practicedOn, certificateIssuedAt,
                certificateRef, reference, createdDate, version);
    }

    /** {@code true} si ya se le entrego el certificado al proveedor. */
    public boolean isCertified() {
        return certificateIssuedAt != null;
    }

    /**
     * El mismo valor que la base calcula en {@code municipality_key}: el codigo del
     * municipio, o el centinela cuando la retencion es nacional.
     */
    public String municipalityKey() {
        return municipalityCode == null ? NATIONAL_MUNICIPALITY_KEY : municipalityCode;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void validate(String supplierTaxId, String supplierName,
            SupplierDocumentKind supplierDocType, String supplierInvoiceRef,
            SupplierWithholdingType withholdingType, String concept, BigDecimal taxableBase,
            BigDecimal ratePercent, BigDecimal amount, String municipalityCode, int fiscalYear,
            String fiscalPeriodKey, LocalDate practicedOn, LocalDateTime certificateIssuedAt,
            String certificateRef, String paymentReceiptRef, LocalDateTime createdDate) {
        requireText("supplierTaxId", supplierTaxId, MAX_TAX_ID_LENGTH);
        requireText("supplierName", supplierName, MAX_SUPPLIER_NAME_LENGTH);
        if (supplierDocType == null)
            throw new IllegalArgumentException("supplierDocType is required");
        requireText("supplierInvoiceRef", supplierInvoiceRef, MAX_INVOICE_REF_LENGTH);
        if (withholdingType == null)
            throw new IllegalArgumentException("withholdingType is required");
        requireText("concept", concept, MAX_CONCEPT_LENGTH);
        validateAmounts(taxableBase, amount);
        validateRate(ratePercent);
        validateMunicipality(withholdingType, municipalityCode);
        validatePeriod(withholdingType, fiscalYear, fiscalPeriodKey);
        if (practicedOn == null)
            throw new IllegalArgumentException("practicedOn is required");
        validateCertificate(certificateIssuedAt, certificateRef);
        if (paymentReceiptRef != null && (paymentReceiptRef.isBlank()
                || paymentReceiptRef.length() > MAX_PAYMENT_RECEIPT_REF_LENGTH))
            throw new IllegalArgumentException(
                    "paymentReceiptRef must be 1 to 255 chars when present");
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    private static void requireText(String field, String value, int maxLength) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(field + " is required");
        if (value.length() > maxLength)
            throw new IllegalArgumentException(field + " must be " + maxLength + " chars or less");
    }

    /**
     * Espejo de {@code chk_sw_amounts}. <strong>{@code amount <= taxableBase} es la
     * comprobacion que importa</strong>: una retencion mayor que la base es un
     * calculo invertido —tarifa aplicada como fraccion, o base y retenido cruzados—
     * y sin esta linea entraria y se declararia.
     */
    private static void validateAmounts(BigDecimal taxableBase, BigDecimal amount) {
        requirePositive("taxableBase", taxableBase);
        requirePositive("amount", amount);
        if (amount.compareTo(taxableBase) > 0)
            throw new IllegalArgumentException(
                    "amount " + amount + " must not exceed the taxable base " + taxableBase);
    }

    private static void requirePositive(String field, BigDecimal value) {
        if (value == null)
            throw new IllegalArgumentException(field + " is required");
        if (value.signum() <= 0)
            throw new IllegalArgumentException(field + " must be greater than zero");
        if (value.scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException(field + " must have 2 decimals or fewer");
    }

    /**
     * Espejo de {@code chk_sw_rate} mas la escala, que la constraint no puede
     * expresar: {@code DECIMAL(9,6)} no rechaza un septimo decimal, lo
     * <em>redondea</em>, y la tarifa guardada deja de ser la aplicada.
     */
    private static void validateRate(BigDecimal ratePercent) {
        if (ratePercent == null)
            throw new IllegalArgumentException("ratePercent is required");
        if (ratePercent.signum() <= 0)
            throw new IllegalArgumentException("ratePercent must be greater than zero");
        if (ratePercent.compareTo(MAX_RATE_PERCENT) > 0)
            throw new IllegalArgumentException("ratePercent must not exceed 100");
        if (ratePercent.scale() > MAX_RATE_SCALE)
            throw new IllegalArgumentException("ratePercent must have 6 decimals or fewer");
    }

    /** Espejo de {@code chk_sw_municipality}, con las <b>dos</b> ramas. */
    private static void validateMunicipality(SupplierWithholdingType withholdingType,
            String municipalityCode) {
        if (withholdingType == SupplierWithholdingType.ICA) {
            if (municipalityCode == null || municipalityCode.isBlank())
                throw new IllegalArgumentException("municipalityCode is required for ICA");
            if (municipalityCode.length() != MUNICIPALITY_CODE_LENGTH)
                throw new IllegalArgumentException("municipalityCode must be 5 characters");
            return;
        }
        if (municipalityCode != null)
            throw new IllegalArgumentException(
                    "municipalityCode must be absent unless the withholding type is ICA");
    }

    /**
     * Espejo de {@code chk_sw_period}.
     *
     * <p>
     * <strong>{@code INCOME_TAX} es MENSUAL aqui y ANUAL en
     * {@code document_withholdings}</strong>, y no es un descuido: la retencion que
     * te practican se imputa al año gravable de tu renta; la que tu practicas se
     * declara en la retencion en la fuente, que es mensual. Va escrito para que el
     * primer lector no lo «corrija».
     */
    private static void validatePeriod(SupplierWithholdingType withholdingType, int fiscalYear,
            String fiscalPeriodKey) {
        if (fiscalYear < MIN_FISCAL_YEAR || fiscalYear > MAX_FISCAL_YEAR)
            throw new IllegalArgumentException("fiscalYear must be between 2020 and 2100");
        if (fiscalPeriodKey == null || fiscalPeriodKey.isBlank())
            throw new IllegalArgumentException("fiscalPeriodKey is required");
        if (!fiscalPeriodKey.startsWith(String.valueOf(fiscalYear)))
            throw new IllegalArgumentException("fiscalPeriodKey " + fiscalPeriodKey
                    + " does not start with the fiscal year " + fiscalYear);
        boolean valid = withholdingType == SupplierWithholdingType.INCOME_TAX
                ? MONTHLY.matcher(fiscalPeriodKey).matches()
                : BIMONTHLY.matcher(fiscalPeriodKey).matches();
        if (!valid)
            throw new IllegalArgumentException("fiscalPeriodKey " + fiscalPeriodKey
                    + " does not match the shape required for " + withholdingType
                    + (withholdingType == SupplierWithholdingType.INCOME_TAX
                            ? " (monthly, yyyy-Mnn)"
                            : " (bimonthly, yyyy-B0n)"));
    }

    /**
     * Espejo de {@code chk_sw_certificate}: la fecha y la referencia van juntas,
     * las dos o ninguna. Una fecha sin numero es un certificado que nadie puede
     * localizar; un numero sin fecha es uno que no se sabe cuando se emitio.
     */
    private static void validateCertificate(LocalDateTime certificateIssuedAt,
            String certificateRef) {
        if (certificateRef != null && certificateRef.length() > MAX_CERTIFICATE_REF_LENGTH)
            throw new IllegalArgumentException("certificateRef must be 100 chars or less");
        if ((certificateIssuedAt == null) != (certificateRef == null))
            throw new IllegalArgumentException(
                    "certificateIssuedAt and certificateRef must both be present or both absent");
    }

    public Long getId() {
        return id;
    }

    public String getSupplierTaxId() {
        return supplierTaxId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public SupplierDocumentKind getSupplierDocType() {
        return supplierDocType;
    }

    public String getSupplierInvoiceRef() {
        return supplierInvoiceRef;
    }

    public SupplierWithholdingType getWithholdingType() {
        return withholdingType;
    }

    public String getConcept() {
        return concept;
    }

    public BigDecimal getTaxableBase() {
        return taxableBase;
    }

    public BigDecimal getRatePercent() {
        return ratePercent;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public String getFiscalPeriodKey() {
        return fiscalPeriodKey;
    }

    public LocalDate getPracticedOn() {
        return practicedOn;
    }

    public LocalDateTime getCertificateIssuedAt() {
        return certificateIssuedAt;
    }

    public String getCertificateRef() {
        return certificateRef;
    }

    public String getPaymentReceiptRef() {
        return paymentReceiptRef;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
