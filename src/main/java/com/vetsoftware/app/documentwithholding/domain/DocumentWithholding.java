package com.vetsoftware.app.documentwithholding.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * La retencion que te practico el cliente sobre una factura de cobro.
 *
 * <p>
 * <strong>No es un descuento ni un impago: es plata propia que fue directa a la
 * DIAN.</strong> El asiento lo dice — una retencion no reduce el ingreso, baja
 * la cartera y sube un activo, porque es un anticipo del impuesto de renta
 * propio. De ahi que la factura quede saldada por este importe aunque el dinero
 * nunca haya entrado a la caja.
 *
 * <p>
 * <strong>Solo se agrega, y por eso no hay {@code enabled}.</strong> Una
 * retencion mal registrada no se desactiva ni se edita: se corrige con otra
 * fila, y las dos quedan. La unica escritura posterior que la tabla admite es
 * apuntarla a su certificado ({@link #linkTo}), y es exactamente la que
 * justifica el {@code version} de la fila.
 *
 * <p>
 * <strong>{@code certificateId} nulable no es un dato que falta: es un estado
 * del negocio.</strong> Mientras no apunte a ningun certificado, la retencion
 * esta <em>sin respaldo</em> — la practicaron, pero no hay papel con el que
 * imputarla. La consulta de vigilancia es una resta entre lo retenido y lo
 * certificado, y esa diferencia es exactamente lo que hay que reclamarle al
 * cliente antes de que venza el plazo.
 */
public class DocumentWithholding {

    /** Espejo de {@code chk_document_withholdings_year}. */
    private static final int MIN_FISCAL_YEAR = 2020;
    private static final int MAX_FISCAL_YEAR = 2100;

    private static final BigDecimal MAX_RATE_PERCENT = new BigDecimal("100");

    /** Renta es anual: {@code 2026-A}. */
    private static final Pattern ANNUAL_PERIOD = Pattern.compile("^\\d{4}-A$");

    /** IVA e ICA se imputan por bimestre: {@code 2026-B01}..{@code 2026-B06}. */
    private static final Pattern BIMONTHLY_PERIOD = Pattern.compile("^\\d{4}-B0[1-6]$");

    /** DIVIPOLA son cinco digitos: dos de departamento y tres de municipio. */
    private static final Pattern DANE_CODE = Pattern.compile("^\\d{5}$");

    private final Long id;
    private final Long companyId;

    /** La factura de cobro sobre la que se practico. */
    private final Long billingDocumentId;

    private final WithholdingType type;

    /** Sobre cuanto se calculo. */
    private final BigDecimal taxableBase;

    /**
     * <strong>Porcentaje, no fraccion.</strong> Ver {@link #getRatePercent()}: la
     * unidad esta en el nombre porque perderla cuesta dinero.
     */
    private final BigDecimal ratePercent;

    /** Lo efectivamente retenido, que es lo que la factura da por saldado. */
    private final BigDecimal amount;

    /** Municipio DIVIPOLA. Obligatorio en ICA y prohibido en los otros dos. */
    private final String municipalityCode;

    /** El ano gravable: lo que decide el derecho a descontarla. */
    private final int fiscalYear;

    /** El periodo exacto en que se declara. Ver {@link #validatePeriod}. */
    private final String fiscalPeriodKey;

    private final LocalDate practicedOn;

    /** Nulo mientras la retencion no tenga respaldo documental. */
    private final Long certificateId;

    private final LocalDateTime createdDate;
    private final Long version;

    public DocumentWithholding(Long id, Long companyId, Long billingDocumentId,
            WithholdingType type, BigDecimal taxableBase, BigDecimal ratePercent, BigDecimal amount,
            String municipalityCode, int fiscalYear, String fiscalPeriodKey, LocalDate practicedOn,
            Long certificateId, LocalDateTime createdDate, Long version) {
        validate(companyId, billingDocumentId, type, taxableBase, ratePercent, amount,
                municipalityCode, fiscalYear, fiscalPeriodKey, practicedOn);
        this.id = id;
        this.companyId = companyId;
        this.billingDocumentId = billingDocumentId;
        this.type = type;
        this.taxableBase = taxableBase;
        this.ratePercent = ratePercent;
        this.amount = amount;
        this.municipalityCode = municipalityCode;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriodKey = fiscalPeriodKey;
        this.practicedOn = practicedOn;
        this.certificateId = certificateId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Retencion recien practicada, todavia <strong>sin respaldo</strong>: nace con
     * {@code certificateId} nulo a proposito, porque el certificado lo expide el
     * cliente mas tarde —normalmente una vez al ano— y hasta que llegue esta fila
     * es cartera que hay que reclamar.
     */
    public static DocumentWithholding register(Long companyId, Long billingDocumentId,
            WithholdingType type, BigDecimal taxableBase, BigDecimal ratePercent, BigDecimal amount,
            String municipalityCode, int fiscalYear, String fiscalPeriodKey, LocalDate practicedOn,
            LocalDateTime createdDate) {
        return new DocumentWithholding(null, companyId, billingDocumentId, type, taxableBase,
                ratePercent, amount, municipalityCode, fiscalYear, fiscalPeriodKey, practicedOn,
                null, createdDate, null);
    }

    /**
     * Apunta la retencion a su certificado, que es lo que la vuelve descontable.
     *
     * <p>
     * <strong>Devuelve una instancia nueva y no muta esta</strong>, en linea con
     * una tabla que solo se agrega: lo unico que cambia entre las dos es la
     * columna, y el {@code version} viaja intacto para que el bloqueo optimista
     * pueda hacer su trabajo en el {@code UPDATE}.
     *
     * <p>
     * <strong>Volver a apuntarla a OTRO certificado es un conflicto, no una
     * correccion.</strong> El certificado es la prueba con la que se imputa la
     * retencion; repuntarla en silencio dejaria dos anos declarados contra papeles
     * distintos sin rastro de cual fue. Repetir el mismo certificado, en cambio, es
     * idempotente: cubre el reintento del operador sin castigarlo.
     */
    public DocumentWithholding linkTo(Long newCertificateId) {
        if (newCertificateId == null)
            throw new IllegalArgumentException("certificateId is required");
        if (certificateId != null && certificateId.equals(newCertificateId))
            return this;
        if (certificateId != null)
            throw new WithholdingAlreadyCertifiedException(id, certificateId, newCertificateId);
        return new DocumentWithholding(id, companyId, billingDocumentId, type, taxableBase,
                ratePercent, amount, municipalityCode, fiscalYear, fiscalPeriodKey, practicedOn,
                newCertificateId, createdDate, version);
    }

    /** Retencion practicada y aun sin papel: lo que hay que reclamar. */
    public boolean isUncertified() {
        return certificateId == null;
    }

    private static void validate(Long companyId, Long billingDocumentId, WithholdingType type,
            BigDecimal taxableBase, BigDecimal ratePercent, BigDecimal amount,
            String municipalityCode, int fiscalYear, String fiscalPeriodKey,
            LocalDate practicedOn) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (billingDocumentId == null)
            throw new IllegalArgumentException("billingDocumentId is required");
        if (type == null)
            throw new IllegalArgumentException("type is required");
        validateAmounts(taxableBase, amount);
        validateRate(ratePercent);
        validateFiscalYear(fiscalYear);
        validatePeriod(type, fiscalYear, fiscalPeriodKey);
        validateMunicipality(type, municipalityCode);
        if (practicedOn == null)
            throw new IllegalArgumentException("practicedOn is required");
    }

    /** Espejo de {@code chk_document_withholdings_amounts}. */
    private static void validateAmounts(BigDecimal taxableBase, BigDecimal amount) {
        if (taxableBase == null)
            throw new IllegalArgumentException("taxableBase is required");
        if (taxableBase.signum() <= 0)
            throw new IllegalArgumentException("taxableBase must be greater than zero");
        if (amount == null)
            throw new IllegalArgumentException("amount is required");
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("amount must be greater than zero");
        // Retener mas de lo que se calculo no es un redondeo: es plata que sale sin
        // base que la sostenga ante la DIAN.
        if (amount.compareTo(taxableBase) > 0)
            throw new IllegalArgumentException("amount must not exceed taxableBase");
    }

    /**
     * Espejo de {@code chk_document_withholdings_rate}, sobre un valor que es
     * <strong>porcentaje</strong>. El tope es 100 y no 1: una tarifa expresada como
     * fraccion pasaria este control sin una queja y retendria cien veces menos.
     */
    private static void validateRate(BigDecimal ratePercent) {
        if (ratePercent == null)
            throw new IllegalArgumentException("ratePercent is required");
        if (ratePercent.signum() <= 0)
            throw new IllegalArgumentException("ratePercent must be greater than zero");
        if (ratePercent.compareTo(MAX_RATE_PERCENT) > 0)
            throw new IllegalArgumentException("ratePercent must not exceed 100");
    }

    /**
     * <strong>Sin ano gravable no hay retencion.</strong> No es un metadato de
     * archivo: es lo que decide el derecho a descontarla. Una retencion sin ano no
     * se puede imputar a ninguna declaracion, asi que se paga dos veces el mismo
     * impuesto — una via retencion y otra al declarar. El rango replica
     * {@code chk_document_withholdings_year}, que ademas caza el error tipico de
     * escribir el ano con dos digitos.
     */
    private static void validateFiscalYear(int fiscalYear) {
        if (fiscalYear < MIN_FISCAL_YEAR || fiscalYear > MAX_FISCAL_YEAR)
            throw new IllegalArgumentException(
                    "fiscalYear must be between " + MIN_FISCAL_YEAR + " and " + MAX_FISCAL_YEAR);
    }

    /**
     * Espejo de {@code chk_document_withholdings_period}, y la regla que motivo el
     * cambio de granularidad de toda la ficha.
     *
     * <p>
     * <strong>Guardar solo el ano era la granularidad equivocada para dos tercios
     * de la tabla.</strong> De los tres tipos, solo la renta es anual; el IVA y el
     * ICA se imputan <em>por bimestre</em>. Con el ano a secas no se puede armar
     * <em>ninguna</em> de las dos declaraciones bimestrales: habria que reconstruir
     * el bimestre a mano desde {@code practicedOn}, y esa reconstruccion no cuadra
     * con lo que efectivamente se presento —una retencion practicada el 1 de marzo
     * puede pertenecer al bimestre anterior—.
     *
     * <p>
     * De ahi la forma segun el tipo: {@code YYYY-A} para la anual y
     * {@code YYYY-B01}..{@code YYYY-B06} para las bimestrales. Y el ano dentro de
     * la clave <strong>tiene que coincidir</strong> con {@code fiscalYear}: sin esa
     * comprobacion, {@code fiscalYear = 2026} con {@code '2025-B03'} entraria y la
     * retencion se declararia en un periodo de otro ano.
     */
    private static void validatePeriod(WithholdingType type, int fiscalYear,
            String fiscalPeriodKey) {
        if (fiscalPeriodKey == null || fiscalPeriodKey.isBlank())
            throw new IllegalArgumentException("fiscalPeriodKey is required");
        Pattern expected = type == WithholdingType.INCOME_TAX ? ANNUAL_PERIOD : BIMONTHLY_PERIOD;
        if (!expected.matcher(fiscalPeriodKey).matches())
            throw new IllegalArgumentException(
                    "fiscalPeriodKey does not match the granularity of " + type);
        if (!fiscalPeriodKey.startsWith(Integer.toString(fiscalYear)))
            throw new IllegalArgumentException(
                    "fiscalPeriodKey year must match fiscalYear " + fiscalYear);
    }

    /**
     * Espejo de {@code chk_document_withholdings_municipality}: el municipio es
     * obligatorio <strong>si y solo si</strong> el tipo es ICA. La tarifa de
     * industria y comercio cambia de municipio a municipio, asi que sin saber cual
     * no se puede verificar nada; y ponerlo en una retencion nacional afirmaria un
     * hecho falso.
     *
     * <p>
     * <strong>El centinela de la unicidad vive en la base y explica por que esta
     * regla es tan estricta.</strong> {@code uq_document_withholdings_case} no se
     * construye sobre {@code municipality_code} sino sobre
     * {@code municipality_key}, una columna generada que sustituye el vacio por
     * {@code '-'}: en un indice unico de MySQL dos {@code NULL} no chocan entre si,
     * de modo que sin el centinela dos retenciones nacionales del mismo documento y
     * del mismo tipo habrian cabido las dos.
     */
    private static void validateMunicipality(WithholdingType type, String municipalityCode) {
        if (type == WithholdingType.ICA) {
            if (municipalityCode == null || municipalityCode.isBlank())
                throw new IllegalArgumentException("municipalityCode is required for ICA");
            if (!DANE_CODE.matcher(municipalityCode).matches())
                throw new IllegalArgumentException("municipalityCode must be five digits");
            return;
        }
        if (municipalityCode != null)
            throw new IllegalArgumentException("municipalityCode is only allowed for ICA");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public WithholdingType getType() {
        return type;
    }

    public BigDecimal getTaxableBase() {
        return taxableBase;
    }

    /**
     * La tarifa aplicada, <strong>en porcentaje y con seis decimales</strong>.
     *
     * <p>
     * <strong>La unidad esta en el nombre porque perderla cuesta dinero.</strong>
     * Las tarifas de industria y comercio se expresan <em>por mil</em>: 6,9 por mil
     * es {@code 0.690000} por ciento, no {@code 6.9}. Y los seis decimales tampoco
     * son de adorno — con dos, {@code 0.69} se conserva pero {@code 4.14} por mil
     * (que es {@code 0.414000} por ciento) se redondea a {@code 0.41}, y base por
     * tarifa deja de dar el importe certificado. El error va siempre en la misma
     * direccion y no salta ninguna alarma: se retiene de menos, en silencio, en
     * cada factura.
     */
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

    public Long getCertificateId() {
        return certificateId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
