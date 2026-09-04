package com.vetsoftware.app.externalinvoicereconciliation.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Lo que Lumbre calculo, enfrentado a lo que se emitio.
 *
 * <p>
 * La factura de la suscripcion <strong>no la emite este software</strong>: la
 * emite el contador o un sistema de facturacion externo, y aqui solo se
 * registra su referencia. Eso deja una rotura de trazabilidad que ninguna clave
 * foranea puede coser -la otra mitad vive en otro sistema-, asi que o se
 * registra la comparacion o no existe.
 *
 * <p>
 * <strong>Cuatro numeros enfrentados, y el impuesto aparte a
 * proposito.</strong> {@code computedTotal}/{@code computedTax} son los
 * propios; {@code externalTotal}/{@code externalTax} los del tercero. Sin
 * separar impuesto de total no se puede saber si el descuadre es de base o de
 * calculo, que es justo la pregunta que decide si hay que llamar a alguien.
 *
 * <p>
 * <strong>El estado inicial es
 * {@link ExternalInvoiceReconciliationStatus#MISSING_EXTERNAL}, y no es un
 * valor mas de la lista.</strong> Es el documento de cobro devengado que nunca
 * recibio factura externa: dinero que Lumbre ya se apunto y que <em>nadie
 * facturo</em>. Es el peor de los cuatro estados y el mas facil de no ver,
 * <strong>porque no produce ninguna diferencia que llame la atencion</strong> —
 * los otros tres saltan solos en cualquier listado de descuadres; este no tiene
 * con que compararse y no aparece en ninguno. Por eso su bandeja tiene caso de
 * uso propio.
 *
 * <p>
 * <strong>{@code postingPeriod} no tiene clave foranea, y es una carencia
 * declarada y no un olvido.</strong> La ficha lo define apuntando a
 * {@code accounting_periods}, que es de otra capa y <em>no existe en ningun
 * changeset del arbol</em>. Queda como clave de periodo con el formato
 * comprobado aqui y en {@code chk_eir_resolved}; atarla es trabajo del
 * changeset que cree los periodos contables.
 */
public class ExternalInvoiceReconciliation {

    /**
     * <strong>Dos pesos.</strong> La unica razon por la que aparece una diferencia
     * de este tamano es aritmetica y esta medida: el total propio se calcula
     * <strong>una vez sobre la base agregada</strong> y el emisor externo lo
     * calcula <strong>linea a linea</strong>, asi que los dos redondeos al centavo
     * no caen en el mismo sitio. Por eso esto es una <em>tolerancia</em> y no una
     * <em>discrepancia</em>: no se esta perdonando un error, se esta reconociendo
     * que dos formas legitimas de sumar el mismo impuesto no dan el mismo ultimo
     * centavo.
     *
     * <p>
     * Y por eso el impuesto se guarda aparte del total: con los dos numeros se
     * puede decir si el descuadre es de base -falta o sobra un concepto- o de
     * calculo -la misma base, redondeada distinto-. Con solo el total, no.
     *
     * <p>
     * <strong>Se compara siempre con {@code compareTo}, nunca con
     * {@code equals}</strong>: {@code new BigDecimal("2.00")} y
     * {@code new BigDecimal("2.0")} son el mismo numero y NO son {@code equals}
     * -difieren en la escala-, asi que un {@code equals} aqui clasificaria como
     * {@code MISMATCH} un descuadre que esta justo en el limite, dependiendo de
     * cuantos ceros trajera el JSON del tercero.
     */
    public static final BigDecimal TOLERANCIA = new BigDecimal("2.00");

    /**
     * Formato del periodo contable, espejo del {@code REGEXP} de
     * {@code chk_eir_resolved}. El mes va acotado a {@code 01..12}: sin esa mitad,
     * {@code 2026-13} pasaria por bueno y el ajuste se imputaria a un cierre que no
     * existe.
     */
    private static final Pattern POSTING_PERIOD = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    private static final int MAX_EXTERNAL_RESOLUTION_NUMBER_LENGTH = 60;
    private static final int MAX_EXTERNAL_INVOICE_ID_LENGTH = 60;
    private static final int MAX_EXTERNAL_CUFE_LENGTH = 100;
    private static final int MAX_RESOLUTION_NOTE_LENGTH = 255;

    /** Los importes viven en {@code DECIMAL(19,2)}. Ver {@link #validateAmount}. */
    private static final int AMOUNT_SCALE = 2;

    private final Long id;
    private final Long companyId;

    /**
     * El documento de cobro conciliado. La FK contra el es COMPUESTA con la
     * empresa.
     */
    private final Long billingDocumentId;

    private String externalResolutionNumber;
    private Integer externalRangeFrom;
    private Integer externalRangeTo;
    private LocalDate resolutionValidUntil;

    private String externalInvoiceId;
    private String externalCufe;

    private final BigDecimal computedTotal;
    private final BigDecimal computedTax;

    private BigDecimal externalTotal;
    private BigDecimal externalTax;
    private BigDecimal difference;

    private ExternalInvoiceReconciliationStatus status;

    private Long resolvedBySystemUserId;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private String postingPeriod;

    private final LocalDateTime createdDate;
    private final Long version;

    public ExternalInvoiceReconciliation(Long id, Long companyId, Long billingDocumentId,
            String externalResolutionNumber, Integer externalRangeFrom, Integer externalRangeTo,
            LocalDate resolutionValidUntil, String externalInvoiceId, String externalCufe,
            BigDecimal computedTotal, BigDecimal computedTax, BigDecimal externalTotal,
            BigDecimal externalTax, BigDecimal difference,
            ExternalInvoiceReconciliationStatus status, Long resolvedBySystemUserId,
            LocalDateTime resolvedAt, String resolutionNote, String postingPeriod,
            LocalDateTime createdDate, Long version) {
        this.id = id;
        this.companyId = companyId;
        this.billingDocumentId = billingDocumentId;
        this.externalResolutionNumber = externalResolutionNumber;
        this.externalRangeFrom = externalRangeFrom;
        this.externalRangeTo = externalRangeTo;
        this.resolutionValidUntil = resolutionValidUntil;
        this.externalInvoiceId = externalInvoiceId;
        this.externalCufe = externalCufe;
        this.computedTotal = computedTotal;
        this.computedTax = computedTax;
        this.externalTotal = externalTotal;
        this.externalTax = externalTax;
        this.difference = difference;
        this.status = status;
        this.resolvedBySystemUserId = resolvedBySystemUserId;
        this.resolvedAt = resolvedAt;
        this.resolutionNote = resolutionNote;
        this.postingPeriod = postingPeriod;
        this.createdDate = createdDate;
        this.version = version;
        validate();
    }

    /**
     * Abre la ficha del documento de cobro devengado, en
     * {@link ExternalInvoiceReconciliationStatus#MISSING_EXTERNAL} y con los dos
     * importes propios.
     *
     * <p>
     * <strong>Nace incompleta a proposito.</strong> Los cuatro campos de la pareja
     * externa van nulos porque todavia no hay factura del tercero, y esa fila vacia
     * <em>es</em> la alarma: mientras siga aqui, hay dinero devengado que nadie
     * facturo. Si la conciliacion se creara solo al llegar la factura externa, el
     * caso que importa -la que no llega nunca- no dejaria ninguna fila y no lo
     * veria nadie.
     */
    public static ExternalInvoiceReconciliation open(Long companyId, Long billingDocumentId,
            BigDecimal computedTotal, BigDecimal computedTax, LocalDateTime createdDate) {
        return new ExternalInvoiceReconciliation(null, companyId, billingDocumentId, null, null,
                null, null, null, null, computedTotal, computedTax, null, null, null,
                ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL, null, null, null, null,
                createdDate, null);
    }

    /**
     * Registra la factura que emitio el tercero, calcula la diferencia y
     * <strong>decide el estado aqui</strong>, no en el caso de uso: la
     * clasificacion es una invariante del negocio y no un paso de la operacion.
     *
     * <p>
     * {@code difference = computedTotal - externalTotal}, exactamente como lo exige
     * {@code chk_eir_difference}. Invertir el orden de la resta pasa el
     * {@code CHECK} de la base solo por casualidad cuando la diferencia es cero, y
     * a partir de ahi rechaza cada escritura con un error de integridad que no
     * senala a la causa.
     *
     * @param externalResolutionNumber
     *            el bloque de numeracion del tercero: numero, rango y vigencia. Van
     *            los cuatro o ninguno ({@code chk_eir_resolution_range}). Es lo
     *            primero que pide un requerimiento sobre facturacion propia y hoy
     *            no se guarda en ninguna parte; con ello se puede avisar ANTES de
     *            que se agote el rango, en vez de descubrirlo el dia que una
     *            factura no sale
     */
    public void match(String externalInvoiceId, String externalCufe, BigDecimal externalTotal,
            BigDecimal externalTax, String externalResolutionNumber, Integer externalRangeFrom,
            Integer externalRangeTo, LocalDate resolutionValidUntil) {
        if (status != ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL)
            throw new ExternalInvoiceAlreadyMatchedException(id, status);
        validateExternalInvoiceId(externalInvoiceId);
        validateCufe(externalCufe);
        validateRequiredAmount("externalTotal", externalTotal);
        validateRequiredAmount("externalTax", externalTax);
        validateNumberingRange(externalResolutionNumber, externalRangeFrom, externalRangeTo,
                resolutionValidUntil);

        BigDecimal calculada = computedTotal.subtract(externalTotal);
        this.externalInvoiceId = externalInvoiceId;
        this.externalCufe = externalCufe;
        this.externalTotal = externalTotal;
        this.externalTax = externalTax;
        this.difference = calculada;
        this.status = classify(calculada);
        this.externalResolutionNumber = externalResolutionNumber;
        this.externalRangeFrom = externalRangeFrom;
        this.externalRangeTo = externalRangeTo;
        this.resolutionValidUntil = resolutionValidUntil;
    }

    /**
     * Cierra el expediente: quien firma, por que, y en que periodo contable se
     * imputa el ajuste. Los cuatro campos van juntos ({@code chk_eir_resolved}).
     *
     * <p>
     * <strong>Tambien se puede resolver una
     * {@link ExternalInvoiceReconciliationStatus#MISSING_EXTERNAL}</strong>, y es
     * deliberado: «este documento no lleva factura externa porque se anulo» es una
     * explicacion legitima, y sin poder escribirla la bandeja de lo que falta se
     * llenaria de ruido permanente hasta que nadie la mirara.
     */
    public void resolve(Long resolvedBySystemUserId, String resolutionNote, String postingPeriod,
            LocalDateTime resolvedAt) {
        if (isResolved())
            throw new ExternalInvoiceReconciliationAlreadyResolvedException(id, this.resolvedAt);
        validateResolution(resolvedBySystemUserId, resolutionNote, postingPeriod, resolvedAt);
        this.resolvedBySystemUserId = resolvedBySystemUserId;
        this.resolutionNote = resolutionNote;
        this.postingPeriod = postingPeriod;
        this.resolvedAt = resolvedAt;
    }

    /**
     * La regla de los tres desenlaces con factura, en un solo sitio.
     *
     * <p>
     * Cero exacto es {@code MATCHED}; hasta {@link #TOLERANCIA} inclusive, en
     * cualquiera de los dos signos, es {@code WITHIN_TOLERANCE}; mas alla es
     * {@code MISMATCH}. La comparacion es {@code compareTo} contra
     * {@code BigDecimal.ZERO} y no {@code equals}: {@code 0.00} y {@code 0} son el
     * mismo numero y no son {@code equals}.
     */
    public static ExternalInvoiceReconciliationStatus classify(BigDecimal difference) {
        if (difference == null)
            throw new IllegalArgumentException("difference is required to classify");
        if (difference.compareTo(BigDecimal.ZERO) == 0)
            return ExternalInvoiceReconciliationStatus.MATCHED;
        return difference.abs().compareTo(TOLERANCIA) <= 0
                ? ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE
                : ExternalInvoiceReconciliationStatus.MISMATCH;
    }

    /** La fila que todavia es dinero devengado que nadie facturo. */
    public boolean isMissingExternal() {
        return status == ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    // --- invariantes --------------------------------------------------------

    private void validate() {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (billingDocumentId == null)
            throw new IllegalArgumentException("billingDocumentId is required");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
        validateRequiredAmount("computedTotal", computedTotal);
        validateRequiredAmount("computedTax", computedTax);
        validateExternalPair();
        validateNumberingRange(externalResolutionNumber, externalRangeFrom, externalRangeTo,
                resolutionValidUntil);
        validateResolutionQuartet();
    }

    /**
     * Espejo de {@code chk_eir_external_pair} y {@code chk_eir_difference}: en
     * {@code MISSING_EXTERNAL} los cuatro campos de la pareja externa van nulos; en
     * cualquier otro estado van los cuatro, y la diferencia es exactamente la
     * resta.
     */
    private void validateExternalPair() {
        if (isMissingExternal()) {
            requireAbsent("externalInvoiceId", externalInvoiceId);
            requireAbsent("externalTotal", externalTotal);
            requireAbsent("externalTax", externalTax);
            requireAbsent("difference", difference);
            return;
        }
        validateExternalInvoiceId(externalInvoiceId);
        validateCufe(externalCufe);
        validateRequiredAmount("externalTotal", externalTotal);
        validateRequiredAmount("externalTax", externalTax);
        if (difference == null)
            throw new IllegalArgumentException("difference is required unless missing external");
        if (difference.compareTo(computedTotal.subtract(externalTotal)) != 0)
            throw new IllegalArgumentException("difference must be computedTotal - externalTotal");
        if (status != classify(difference))
            throw new IllegalArgumentException("status must match the classified difference");
    }

    /** Espejo de {@code chk_eir_resolution_range}: los cuatro o ninguno. */
    private static void validateNumberingRange(String number, Integer from, Integer to,
            LocalDate validUntil) {
        boolean anyPresent = number != null || from != null || to != null || validUntil != null;
        if (!anyPresent)
            return;
        if (number == null || number.isBlank())
            throw new IllegalArgumentException(
                    "externalResolutionNumber is required with the numbering range");
        if (number.length() > MAX_EXTERNAL_RESOLUTION_NUMBER_LENGTH)
            throw new IllegalArgumentException("externalResolutionNumber must be 60 chars or less");
        if (from == null || to == null || validUntil == null)
            throw new IllegalArgumentException(
                    "externalRangeFrom, externalRangeTo and resolutionValidUntil go together");
        if (to < from)
            throw new IllegalArgumentException(
                    "externalRangeTo must be greater than or equal to externalRangeFrom");
    }

    /** Espejo de {@code chk_eir_resolved} sobre el estado ya construido. */
    private void validateResolutionQuartet() {
        boolean anyPresent = resolvedAt != null || resolvedBySystemUserId != null
                || resolutionNote != null || postingPeriod != null;
        if (!anyPresent)
            return;
        validateResolution(resolvedBySystemUserId, resolutionNote, postingPeriod, resolvedAt);
    }

    private static void validateResolution(Long systemUserId, String note, String period,
            LocalDateTime at) {
        if (at == null)
            throw new IllegalArgumentException("resolvedAt is required to resolve");
        if (systemUserId == null)
            throw new IllegalArgumentException("resolvedBySystemUserId is required to resolve");
        if (note == null || note.isBlank())
            throw new IllegalArgumentException("resolutionNote is required to resolve");
        if (note.length() > MAX_RESOLUTION_NOTE_LENGTH)
            throw new IllegalArgumentException("resolutionNote must be 255 chars or less");
        if (period == null || !POSTING_PERIOD.matcher(period).matches())
            throw new IllegalArgumentException("postingPeriod must be YYYY-MM with month 01..12");
    }

    private static void validateExternalInvoiceId(String externalInvoiceId) {
        if (externalInvoiceId == null || externalInvoiceId.isBlank())
            throw new IllegalArgumentException("externalInvoiceId is required");
        if (externalInvoiceId.length() > MAX_EXTERNAL_INVOICE_ID_LENGTH)
            throw new IllegalArgumentException("externalInvoiceId must be 60 chars or less");
    }

    private static void validateCufe(String externalCufe) {
        if (externalCufe != null && externalCufe.length() > MAX_EXTERNAL_CUFE_LENGTH)
            throw new IllegalArgumentException("externalCufe must be 100 chars or less");
    }

    private static void validateRequiredAmount(String field, BigDecimal amount) {
        if (amount == null)
            throw new IllegalArgumentException(field + " is required");
        validateAmount(field, amount);
    }

    /**
     * Espejo de {@code chk_eir_amounts}, mas la escala.
     *
     * <p>
     * <strong>Los decimales de mas no son quisquillosidad.</strong> La columna es
     * {@code DECIMAL(19,2)}: un importe con tres decimales lo redondea la base al
     * escribirlo, y entonces la diferencia guardada deja de ser la resta de los dos
     * numeros guardados — {@code chk_eir_difference} rechaza la fila con un error
     * de integridad que no senala a la causa. Se mira la escala util
     * ({@code stripTrailingZeros}) para no rechazar un {@code 100.000} que es
     * exactamente cien.
     */
    private static void validateAmount(String field, BigDecimal amount) {
        if (amount.signum() < 0)
            throw new IllegalArgumentException(field + " cannot be negative");
        if (amount.stripTrailingZeros().scale() > AMOUNT_SCALE)
            throw new IllegalArgumentException(field + " must have at most 2 decimals");
    }

    private static void requireAbsent(String field, Object value) {
        if (value != null)
            throw new IllegalArgumentException(field + " must be absent when missing external");
    }

    // --- lectura ------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBillingDocumentId() {
        return billingDocumentId;
    }

    public String getExternalResolutionNumber() {
        return externalResolutionNumber;
    }

    public Integer getExternalRangeFrom() {
        return externalRangeFrom;
    }

    public Integer getExternalRangeTo() {
        return externalRangeTo;
    }

    public LocalDate getResolutionValidUntil() {
        return resolutionValidUntil;
    }

    public String getExternalInvoiceId() {
        return externalInvoiceId;
    }

    public String getExternalCufe() {
        return externalCufe;
    }

    public BigDecimal getComputedTotal() {
        return computedTotal;
    }

    public BigDecimal getComputedTax() {
        return computedTax;
    }

    public BigDecimal getExternalTotal() {
        return externalTotal;
    }

    public BigDecimal getExternalTax() {
        return externalTax;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public ExternalInvoiceReconciliationStatus getStatus() {
        return status;
    }

    public Long getResolvedBySystemUserId() {
        return resolvedBySystemUserId;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public String getPostingPeriod() {
        return postingPeriod;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
