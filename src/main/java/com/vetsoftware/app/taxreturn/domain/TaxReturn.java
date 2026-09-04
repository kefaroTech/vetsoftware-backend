package com.vetsoftware.app.taxreturn.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Lo que Lumbre declaro ante la DIAN o el municipio, y hasta cuando pueden
 * revisarlo.
 *
 * <h2>Sin empresa: son declaraciones de Lumbre, no de la clinica</h2>
 *
 * <p>
 * Cero superficie de cliente. {@code tax_returns} no tiene columna
 * {@code company_id} y {@code TaxReturnJpaEntity} no alcanza
 * {@code CompanyJpaEntity} por ninguna asociacion; si la alcanzara, las cuatro
 * reglas duras de aislamiento de BE-COV caerian sobre la feature entera.
 *
 * <h2>{@code firmezaUntil} es la columna de la que cuelga toda la
 * conservacion</h2>
 *
 * <p>
 * {@code chk_tax_returns_filed} hace que exista <b>siempre</b> que la
 * declaracion este presentada. No es un dato decorativo: el termino de
 * conservacion de los soportes <em>es</em> el termino de firmeza de la
 * declaracion de renta que sostienen (art. 632 ET, modificado por el art. 46 de
 * la Ley 962 de 2005), no un numero escrito en un changeset. La firmeza general
 * son tres años desde el vencimiento del plazo para declarar (art. 714 ET), y
 * cinco si hay compensacion o determinacion de perdidas fiscales o regimen de
 * precios de transferencia.
 *
 * <p>
 * <strong>Cual de las dos aplica a Lumbre sigue sin confirmar por un
 * contador</strong>, y son dos años de diferencia sobre la purga de
 * {@code company_usage_events}. Por eso {@code firmezaUntil} es un dato que se
 * escribe y no una formula que se calcula aqui: el dominio no puede decidir
 * algo que nadie ha decidido.
 *
 * <h2>Las declaraciones no se editan: se suceden</h2>
 *
 * <p>
 * {@code sequenceNumber} es 1 en la inicial y sube con cada correccion, y
 * {@code chk_tax_returns_correction} ata las dos mitades: la 1 no corrige a
 * nadie, las demas corrigen a alguien. {@code uq_tax_returns_case} lleva el
 * numero dentro —sin el, una correccion <b>no cabria</b>— y
 * {@code uq_tax_returns_current}, sobre la columna generada
 * {@code current_return_marker}, garantiza que solo hay <b>una vigente</b> por
 * impuesto, periodo y municipio: al corregir, la anterior pasa a
 * {@link TaxReturnStatus#CORRECTED} y libera el hueco.
 *
 * <h2>El centinela del municipio</h2>
 *
 * <p>
 * {@code municipalityCode} es obligatorio si y solo si el impuesto es
 * {@link TaxKind#ICA}. El dominio guarda {@code null} en las nacionales; el
 * centinela vive en la columna generada {@code municipality_key}, que <b>no se
 * mapea</b>. Sin el, dos declaraciones nacionales del mismo periodo no
 * chocarian en el indice unico —en SQL dos {@code NULL} no son iguales— y la
 * consulta devolveria dos.
 */
public class TaxReturn {

    /** Espejo de {@code chk_tax_returns_year}. */
    private static final int MIN_FISCAL_YEAR = 2020;
    private static final int MAX_FISCAL_YEAR = 2100;

    private static final int MAX_RECEIPT_REF_LENGTH = 100;
    private static final int MAX_FILE_REF_LENGTH = 255;
    private static final int MAX_MUNICIPALITY_CODE_LENGTH = 5;

    /** {@code DECIMAL(19,2)}: un tercer decimal lo redondearia MySQL sin avisar. */
    private static final int MAX_AMOUNT_SCALE = 2;

    /** {@code 2026-M03}: retencion en la fuente, mensual. */
    private static final Pattern MONTHLY = Pattern.compile("^[0-9]{4}-M(0[1-9]|1[0-2])$");

    /** {@code 2026-B03}: bimestre, seis al año. */
    private static final Pattern BIMONTHLY = Pattern.compile("^[0-9]{4}-B0[1-6]$");

    /** {@code 2026-C02}: cuatrimestre, tres al año. */
    private static final Pattern FOURMONTHLY = Pattern.compile("^[0-9]{4}-C0[1-3]$");

    /** El mismo centinela que calcula {@code municipality_key} en la base. */
    public static final String NATIONAL_MUNICIPALITY_KEY = "-";

    private final Long id;
    private final TaxKind taxKind;
    private final int fiscalYear;
    private final String fiscalPeriodKey;
    private final int sequenceNumber;
    private final String municipalityCode;
    private final VatFrequency vatFrequency;
    private final TaxReturnStatus status;
    private final LocalDateTime filedAt;
    private final Long filedBySystemUserId;
    private final String receiptRef;
    private final String fileRef;
    private final BigDecimal totalGenerated;
    private final BigDecimal totalDeductible;
    private final BigDecimal balancePayable;
    private final BigDecimal balanceCredit;
    private final LocalDate firmezaUntil;
    private final Long correctsReturnId;
    private final LocalDateTime createdDate;
    private final Long version;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TaxReturn(Long id, TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
            int sequenceNumber, String municipalityCode, VatFrequency vatFrequency,
            TaxReturnStatus status, LocalDateTime filedAt, Long filedBySystemUserId,
            String receiptRef, String fileRef, BigDecimal totalGenerated,
            BigDecimal totalDeductible, BigDecimal balancePayable, BigDecimal balanceCredit,
            LocalDate firmezaUntil, Long correctsReturnId, LocalDateTime createdDate,
            Long version) {
        validate(id, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber, municipalityCode,
                vatFrequency, status, filedAt, filedBySystemUserId, receiptRef, fileRef,
                totalGenerated, totalDeductible, balancePayable, balanceCredit, firmezaUntil,
                correctsReturnId, createdDate);
        this.id = id;
        this.taxKind = taxKind;
        this.fiscalYear = fiscalYear;
        this.fiscalPeriodKey = fiscalPeriodKey;
        this.sequenceNumber = sequenceNumber;
        this.municipalityCode = municipalityCode;
        this.vatFrequency = vatFrequency;
        this.status = status;
        this.filedAt = filedAt;
        this.filedBySystemUserId = filedBySystemUserId;
        this.receiptRef = receiptRef;
        this.fileRef = fileRef;
        this.totalGenerated = totalGenerated;
        this.totalDeductible = totalDeductible;
        this.balancePayable = balancePayable;
        this.balanceCredit = balanceCredit;
        this.firmezaUntil = firmezaUntil;
        this.correctsReturnId = correctsReturnId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Declaracion inicial en borrador: {@code sequenceNumber = 1} y sin nadie a
     * quien corregir, que son las dos mitades de
     * {@code chk_tax_returns_correction}.
     */
    public static TaxReturn draft(TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
            String municipalityCode, VatFrequency vatFrequency, BigDecimal totalGenerated,
            BigDecimal totalDeductible, BigDecimal balancePayable, BigDecimal balanceCredit,
            LocalDateTime createdDate) {
        return new TaxReturn(null, taxKind, fiscalYear, fiscalPeriodKey, 1, municipalityCode,
                vatFrequency, TaxReturnStatus.DRAFT, null, null, null, null, totalGenerated,
                totalDeductible, balancePayable, balanceCredit, null, null, createdDate, null);
    }

    /**
     * El borrador de la correccion de <b>esta</b> declaracion: mismo supuesto, el
     * numero siguiente y apuntando a ella.
     *
     * <p>
     * <strong>Nace sin id, asi que no puede apuntarse a si misma</strong> —lo que
     * hace que {@link TaxReturnCannotCorrectItselfException} se cumpla hoy de forma
     * vacia por este camino—, pero el constructor lo comprueba igual: es la unica
     * red que hay contra una cadena de correcciones que se cierre sobre si misma, y
     * la base no puede ponerla porque el manual prohibe referenciar una columna
     * {@code AUTO_INCREMENT} dentro de un {@code CHECK}.
     *
     * <p>
     * Corregir es ademas lo que obliga a pasar la anterior a
     * {@link TaxReturnStatus#CORRECTED}: mientras siga {@code DRAFT} o
     * {@code FILED}, {@code uq_tax_returns_current} impide que exista la nueva.
     */
    public TaxReturn correctionDraft(BigDecimal newTotalGenerated, BigDecimal newTotalDeductible,
            BigDecimal newBalancePayable, BigDecimal newBalanceCredit, LocalDateTime createdOn) {
        if (id == null)
            throw new IllegalStateException("only a persisted tax return can be corrected");
        return new TaxReturn(null, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber + 1,
                municipalityCode, vatFrequency, TaxReturnStatus.DRAFT, null, null, null, null,
                newTotalGenerated, newTotalDeductible, newBalancePayable, newBalanceCredit, null,
                id, createdOn, null);
    }

    /**
     * Corrige los importes del borrador.
     *
     * <p>
     * <strong>Se niega en cuanto la declaracion deja de ser borrador</strong>, y
     * esa negativa es toda la barandilla que hay: {@code chk_tax_returns_filed}
     * mira la fila y no de donde venia, asi que reeditar los importes de una
     * presentada produce una fila que el motor acepta —y unos numeros que ya no
     * coinciden con el formulario radicado.
     */
    public TaxReturn updateAmounts(BigDecimal newTotalGenerated, BigDecimal newTotalDeductible,
            BigDecimal newBalancePayable, BigDecimal newBalanceCredit) {
        if (status != TaxReturnStatus.DRAFT)
            throw new TaxReturnNotEditableException(id, status);
        return new TaxReturn(id, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber,
                municipalityCode, vatFrequency, status, filedAt, filedBySystemUserId, receiptRef,
                fileRef, newTotalGenerated, newTotalDeductible, newBalancePayable, newBalanceCredit,
                firmezaUntil, correctsReturnId, createdDate, version);
    }

    /**
     * Presenta la declaracion. Exige las cinco cosas a la vez —fecha, firmante,
     * radicado, copia del fichero y firmeza posterior a la presentacion—, espejo de
     * la segunda rama de {@code chk_tax_returns_filed}.
     *
     * <p>
     * Conservar copia de lo presentado y de su recibo de pago es obligacion expresa
     * del art. 632 ET; por eso {@code fileRef} no es opcional aqui aunque la
     * columna sea nulable en la tabla.
     */
    public TaxReturn file(LocalDateTime on, Long systemUserId, String receipt, String file,
            LocalDate firmeza) {
        if (status != TaxReturnStatus.DRAFT)
            throw new TaxReturnNotEditableException(id, status);
        return new TaxReturn(id, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber,
                municipalityCode, vatFrequency, TaxReturnStatus.FILED, on, systemUserId, receipt,
                file, totalGenerated, totalDeductible, balancePayable, balanceCredit, firmeza,
                correctsReturnId, createdDate, version);
    }

    /**
     * Marca la declaracion como corregida por una posterior. Conserva sus datos de
     * presentacion —fue una declaracion real— y libera el hueco de
     * {@code uq_tax_returns_current}.
     */
    public TaxReturn markCorrected() {
        if (status != TaxReturnStatus.FILED)
            throw new TaxReturnNotEditableException(id, status);
        return new TaxReturn(id, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber,
                municipalityCode, vatFrequency, TaxReturnStatus.CORRECTED, filedAt,
                filedBySystemUserId, receiptRef, fileRef, totalGenerated, totalDeductible,
                balancePayable, balanceCredit, firmezaUntil, correctsReturnId, createdDate,
                version);
    }

    /**
     * Anula el borrador. <strong>Borra los datos de presentacion y la
     * firmeza</strong>, espejo de la primera rama de {@code chk_tax_returns_filed}:
     * una declaracion anulada no sostiene ninguna ventana de conservacion.
     */
    public TaxReturn annul() {
        if (status != TaxReturnStatus.DRAFT)
            throw new TaxReturnNotEditableException(id, status);
        return new TaxReturn(id, taxKind, fiscalYear, fiscalPeriodKey, sequenceNumber,
                municipalityCode, vatFrequency, TaxReturnStatus.ANNULLED, null, null, null, fileRef,
                totalGenerated, totalDeductible, balancePayable, balanceCredit, null,
                correctsReturnId, createdDate, version);
    }

    /** {@code true} si ocupa el hueco de {@code uq_tax_returns_current}. */
    public boolean isCurrent() {
        return status.occupiesTheCurrentSlot();
    }

    /**
     * El mismo valor que la base calcula en {@code municipality_key}: el codigo del
     * municipio, o el centinela cuando la declaracion es nacional.
     */
    public String municipalityKey() {
        return municipalityCode == null ? NATIONAL_MUNICIPALITY_KEY : municipalityCode;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void validate(Long id, TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
            int sequenceNumber, String municipalityCode, VatFrequency vatFrequency,
            TaxReturnStatus status, LocalDateTime filedAt, Long filedBySystemUserId,
            String receiptRef, String fileRef, BigDecimal totalGenerated,
            BigDecimal totalDeductible, BigDecimal balancePayable, BigDecimal balanceCredit,
            LocalDate firmezaUntil, Long correctsReturnId, LocalDateTime createdDate) {
        if (taxKind == null)
            throw new IllegalArgumentException("taxKind is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (fiscalYear < MIN_FISCAL_YEAR || fiscalYear > MAX_FISCAL_YEAR)
            throw new IllegalArgumentException("fiscalYear must be between 2020 and 2100");
        validateMunicipality(taxKind, municipalityCode);
        validateVatFrequency(taxKind, vatFrequency);
        validatePeriodKey(taxKind, fiscalYear, fiscalPeriodKey, vatFrequency);
        validateCorrection(id, sequenceNumber, correctsReturnId);
        validateAmounts(totalGenerated, totalDeductible, balancePayable, balanceCredit);
        validateFiling(status, filedAt, filedBySystemUserId, receiptRef, fileRef, firmezaUntil);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    /**
     * Espejo de {@code chk_tax_returns_municipality}, con las <b>dos</b> ramas. Sin
     * la segunda, una declaracion nacional podria llevar municipio y habria dos
     * filas para el mismo supuesto —una con municipio y otra sin el— que la
     * unicidad no veria como iguales.
     */
    private static void validateMunicipality(TaxKind taxKind, String municipalityCode) {
        if (taxKind == TaxKind.ICA) {
            if (municipalityCode == null || municipalityCode.isBlank())
                throw new IllegalArgumentException("municipalityCode is required for ICA");
            if (municipalityCode.length() != MAX_MUNICIPALITY_CODE_LENGTH)
                throw new IllegalArgumentException("municipalityCode must be 5 characters");
            return;
        }
        if (municipalityCode != null)
            throw new IllegalArgumentException(
                    "municipalityCode must be absent unless the tax kind is ICA");
    }

    /** Espejo de {@code chk_tax_returns_vat_freq}, tambien con las dos ramas. */
    private static void validateVatFrequency(TaxKind taxKind, VatFrequency vatFrequency) {
        if (taxKind == TaxKind.VAT) {
            if (vatFrequency == null)
                throw new IllegalArgumentException("vatFrequency is required for VAT");
            return;
        }
        if (vatFrequency != null)
            throw new IllegalArgumentException(
                    "vatFrequency must be absent unless the tax kind is VAT");
    }

    /**
     * Espejo de {@code chk_tax_returns_period}: la forma de la clave depende del
     * impuesto, y para IVA ademas de su periodicidad.
     *
     * <p>
     * <strong>Es el {@code CHECK} que impide que una retencion de diciembre acabe
     * declarada en el bimestre de enero.</strong> Sin el, la declaracion se
     * presenta fuera de plazo y no hay ningun error que lo delate hasta que llega
     * la sancion por extemporaneidad.
     */
    private static void validatePeriodKey(TaxKind taxKind, int fiscalYear, String fiscalPeriodKey,
            VatFrequency vatFrequency) {
        if (fiscalPeriodKey == null || fiscalPeriodKey.isBlank())
            throw new IllegalArgumentException("fiscalPeriodKey is required");
        String annual = fiscalYear + "-A";
        boolean valid = switch (taxKind) {
            case INCOME_TAX -> fiscalPeriodKey.equals(annual);
            case WITHHOLDING -> MONTHLY.matcher(fiscalPeriodKey).matches()
                    && startsWithYear(fiscalPeriodKey, fiscalYear);
            case ICA -> BIMONTHLY.matcher(fiscalPeriodKey).matches()
                    && startsWithYear(fiscalPeriodKey, fiscalYear);
            case VAT -> startsWithYear(fiscalPeriodKey, fiscalYear)
                    && matchesVatShape(fiscalPeriodKey, vatFrequency, annual);
        };
        if (!valid)
            throw new IllegalArgumentException("fiscalPeriodKey " + fiscalPeriodKey
                    + " does not match the shape required for " + taxKind
                    + (taxKind == TaxKind.VAT ? " with frequency " + vatFrequency : "")
                    + " of fiscal year " + fiscalYear);
    }

    private static boolean matchesVatShape(String fiscalPeriodKey, VatFrequency vatFrequency,
            String annual) {
        return switch (vatFrequency) {
            case BIMONTHLY -> BIMONTHLY.matcher(fiscalPeriodKey).matches();
            case FOURMONTHLY -> FOURMONTHLY.matcher(fiscalPeriodKey).matches();
            case ANNUAL -> fiscalPeriodKey.equals(annual);
        };
    }

    private static boolean startsWithYear(String fiscalPeriodKey, int fiscalYear) {
        return fiscalPeriodKey.length() >= 4
                && fiscalPeriodKey.startsWith(String.valueOf(fiscalYear));
    }

    /**
     * Espejo de {@code chk_tax_returns_sequence} y de
     * {@code chk_tax_returns_correction}, mas la invariante que la base <b>no
     * puede</b> imponer: que la declaracion no se corrija a si misma.
     */
    private static void validateCorrection(Long id, int sequenceNumber, Long correctsReturnId) {
        if (sequenceNumber < 1)
            throw new IllegalArgumentException("sequenceNumber must be 1 or greater");
        if (sequenceNumber == 1 && correctsReturnId != null)
            throw new IllegalArgumentException("the first tax return corrects nothing");
        if (sequenceNumber > 1 && correctsReturnId == null)
            throw new IllegalArgumentException("a correction must name the return it corrects");
        if (id != null && id.equals(correctsReturnId))
            throw new TaxReturnCannotCorrectItselfException(id);
    }

    /**
     * Espejo de {@code chk_tax_returns_amounts} y de
     * {@code chk_tax_returns_balance}, mas la escala, que las constraints no pueden
     * expresar: {@code DECIMAL(19,2)} no rechaza un tercer decimal, lo
     * <em>redondea</em>.
     *
     * <p>
     * Los dos saldos no pueden ser ambos distintos de cero: una declaracion o deja
     * saldo a pagar o deja saldo a favor, nunca las dos cosas.
     */
    private static void validateAmounts(BigDecimal totalGenerated, BigDecimal totalDeductible,
            BigDecimal balancePayable, BigDecimal balanceCredit) {
        requireNonNegative("totalGenerated", totalGenerated);
        requireNonNegative("totalDeductible", totalDeductible);
        requireNonNegative("balancePayable", balancePayable);
        requireNonNegative("balanceCredit", balanceCredit);
        if (balancePayable.signum() != 0 && balanceCredit.signum() != 0)
            throw new IllegalArgumentException("a tax return leaves either a payable balance or a"
                    + " credit balance, never both");
    }

    private static void requireNonNegative(String field, BigDecimal value) {
        if (value == null)
            throw new IllegalArgumentException(field + " is required");
        if (value.signum() < 0)
            throw new IllegalArgumentException(field + " must not be negative");
        if (value.scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException(field + " must have 2 decimals or fewer");
    }

    /**
     * Espejo de {@code chk_tax_returns_filed}, con las <b>dos</b> ramas escritas.
     *
     * <p>
     * La segunda es la que hace que {@code firmezaUntil} exista siempre que la
     * declaracion este presentada — y con ella, la unica forma de saber cuando
     * prescribe la facultad de revision y hasta cuando hay que conservar los
     * soportes que la sostienen.
     */
    private static void validateFiling(TaxReturnStatus status, LocalDateTime filedAt,
            Long filedBySystemUserId, String receiptRef, String fileRef, LocalDate firmezaUntil) {
        if (receiptRef != null && receiptRef.length() > MAX_RECEIPT_REF_LENGTH)
            throw new IllegalArgumentException("receiptRef must be 100 chars or less");
        if (fileRef != null && fileRef.length() > MAX_FILE_REF_LENGTH)
            throw new IllegalArgumentException("fileRef must be 255 chars or less");
        if (!status.isFiled()) {
            if (filedAt != null || filedBySystemUserId != null || receiptRef != null
                    || firmezaUntil != null)
                throw new IllegalArgumentException("a " + status
                        + " tax return must not carry filing data nor a firmeza date");
            return;
        }
        if (filedAt == null || filedBySystemUserId == null || receiptRef == null
                || receiptRef.isBlank() || fileRef == null || fileRef.isBlank()
                || firmezaUntil == null)
            throw new IllegalArgumentException("a filed tax return needs filedAt,"
                    + " filedBySystemUserId, receiptRef, fileRef and firmezaUntil");
        if (!firmezaUntil.isAfter(filedAt.toLocalDate()))
            throw new IllegalArgumentException("firmezaUntil must be after the filing date");
    }

    public Long getId() {
        return id;
    }

    public TaxKind getTaxKind() {
        return taxKind;
    }

    public int getFiscalYear() {
        return fiscalYear;
    }

    public String getFiscalPeriodKey() {
        return fiscalPeriodKey;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public VatFrequency getVatFrequency() {
        return vatFrequency;
    }

    public TaxReturnStatus getStatus() {
        return status;
    }

    public LocalDateTime getFiledAt() {
        return filedAt;
    }

    public Long getFiledBySystemUserId() {
        return filedBySystemUserId;
    }

    public String getReceiptRef() {
        return receiptRef;
    }

    public String getFileRef() {
        return fileRef;
    }

    public BigDecimal getTotalGenerated() {
        return totalGenerated;
    }

    public BigDecimal getTotalDeductible() {
        return totalDeductible;
    }

    public BigDecimal getBalancePayable() {
        return balancePayable;
    }

    public BigDecimal getBalanceCredit() {
        return balanceCredit;
    }

    public LocalDate getFirmezaUntil() {
        return firmezaUntil;
    }

    public Long getCorrectsReturnId() {
        return correctsReturnId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
