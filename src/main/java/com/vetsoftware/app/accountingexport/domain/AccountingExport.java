package com.vetsoftware.app.accountingexport.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * El asiento resumen que se le entrega al software contable: doce filas al año
 * por clase de fichero.
 *
 * <h2>La partida doble, comprobada sobre dos numeros</h2>
 *
 * <p>
 * <strong>Es la unica invariante contable que el documento maestro daba por
 * imposible de imponer</strong>, y al eliminar el diario pasa a ser trivial:
 * {@code totalDebit == totalCredit} y los dos no negativos, espejo de
 * {@code chk_accounting_exports_balanced}. Que cuadren se comprueba
 * <b>antes</b> de exportar, no despues de que el contador devuelva el fichero.
 *
 * <h2>Un solo fichero vivo por mes y clase</h2>
 *
 * <p>
 * {@code attemptNumber} es lo que libera rehacer un fichero rechazado sin abrir
 * la puerta a exportar dos veces lo mismo:
 * {@code uq_accounting_exports_attempt} es
 * {@code (period_key, export_kind, attempt_number)}. Y la columna generada
 * {@code current_export_marker} —que <b>no estaba en el documento</b>— es la
 * que hace que «rehacer» signifique algo: mientras el estado es
 * {@code GENERATED} o {@code DELIVERED} el marcador vale {@code periodo|clase}
 * y {@code uq_accounting_exports_current} rechaza el duplicado; en cuanto pasa
 * a {@code REJECTED} o {@code SUPERSEDED}, el marcador se vuelve {@code NULL} y
 * libera el hueco. {@link AccountingExportStatus#occupiesTheCurrentSlot()} es
 * el espejo Java de esa lista.
 *
 * <h2>Catalogo global: aqui no hay empresa</h2>
 *
 * <p>
 * Los libros son de VetSoftware. La tabla no tiene {@code company_id} y
 * {@code AccountingExportJpaEntity} no alcanza {@code CompanyJpaEntity} por
 * ninguna asociacion; si la alcanzara, las cuatro reglas duras de aislamiento
 * de BE-COV caerian sobre la feature entera.
 */
public class AccountingExport {

    /** Mismo {@code REGEXP} mensual que {@code chk_accounting_periods_key}. */
    private static final Pattern MONTHLY_KEY = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    /** Espejo de {@code chk_accounting_exports_hash}: SHA-256 en minusculas. */
    private static final Pattern SHA256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    private static final int MAX_FILE_REF_LENGTH = 255;
    private static final int MAX_REJECTION_REASON_LENGTH = 255;

    /** {@code DECIMAL(19,2)}: un tercer decimal lo redondearia MySQL sin avisar. */
    private static final int MAX_AMOUNT_SCALE = 2;

    private final Long id;
    private final String periodKey;
    private final AccountingExportKind exportKind;
    private final int attemptNumber;
    private final AccountingExportStatus status;
    private final LocalDateTime generatedAt;
    private final Long generatedBySystemUserId;
    private final BigDecimal totalDebit;
    private final BigDecimal totalCredit;
    private final String totalsHash;
    private final String fileRef;
    private final LocalDateTime deliveredAt;
    private final LocalDateTime rejectedAt;
    private final String rejectionReason;
    private final LocalDateTime createdDate;
    private final Long version;

    public AccountingExport(Long id, String periodKey, AccountingExportKind exportKind,
            int attemptNumber, AccountingExportStatus status, LocalDateTime generatedAt,
            Long generatedBySystemUserId, BigDecimal totalDebit, BigDecimal totalCredit,
            String totalsHash, String fileRef, LocalDateTime deliveredAt, LocalDateTime rejectedAt,
            String rejectionReason, LocalDateTime createdDate, Long version) {
        validate(periodKey, exportKind, attemptNumber, status, generatedAt, generatedBySystemUserId,
                totalDebit, totalCredit, totalsHash, fileRef, deliveredAt, rejectedAt,
                rejectionReason, createdDate);
        this.id = id;
        this.periodKey = periodKey;
        this.exportKind = exportKind;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.generatedAt = generatedAt;
        this.generatedBySystemUserId = generatedBySystemUserId;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.totalsHash = totalsHash;
        this.fileRef = fileRef;
        this.deliveredAt = deliveredAt;
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Exportacion recien generada. Nace en {@code GENERATED} —el defecto de la
     * columna— y sin version: la asigna Hibernate al insertar.
     */
    public static AccountingExport generate(String periodKey, AccountingExportKind exportKind,
            int attemptNumber, LocalDateTime generatedAt, Long generatedBySystemUserId,
            BigDecimal totalDebit, BigDecimal totalCredit, String totalsHash, String fileRef,
            LocalDateTime createdDate) {
        return new AccountingExport(null, periodKey, exportKind, attemptNumber,
                AccountingExportStatus.GENERATED, generatedAt, generatedBySystemUserId, totalDebit,
                totalCredit, totalsHash, fileRef, null, null, null, createdDate, null);
    }

    /**
     * El contador lo recibio. <strong>Se niega si el fichero ya tenia
     * desenlace</strong>: la base no lo impide —el {@code CHECK} mira la fila, no
     * de donde venia— y sin esta comprobacion un {@code REJECTED} tardio borraria
     * la fecha de entrega, que es la prueba de que el mes se entrego a tiempo.
     */
    public AccountingExport markDelivered(LocalDateTime on) {
        requirePending();
        if (on == null || on.isBefore(generatedAt))
            throw new IllegalArgumentException("deliveredAt must not be before generatedAt");
        return new AccountingExport(id, periodKey, exportKind, attemptNumber,
                AccountingExportStatus.DELIVERED, generatedAt, generatedBySystemUserId, totalDebit,
                totalCredit, totalsHash, fileRef, on, null, null, createdDate, version);
    }

    /**
     * El contador lo devolvio. Exige motivo escrito, espejo de la tercera rama de
     * {@code chk_accounting_exports_lifecycle}: un rechazo sin motivo obliga a
     * rehacer el fichero a ciegas.
     *
     * <p>
     * Es ademas lo que <b>libera el hueco</b> de
     * {@code uq_accounting_exports_current} para el siguiente intento.
     */
    public AccountingExport markRejected(LocalDateTime on, String reason) {
        requirePending();
        if (on == null || on.isBefore(generatedAt))
            throw new IllegalArgumentException("rejectedAt must not be before generatedAt");
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("rejectionReason is required when rejecting");
        return new AccountingExport(id, periodKey, exportKind, attemptNumber,
                AccountingExportStatus.REJECTED, generatedAt, generatedBySystemUserId, totalDebit,
                totalCredit, totalsHash, fileRef, null, on, reason, createdDate, version);
    }

    /**
     * Reemplazado por un intento posterior.
     *
     * <p>
     * <strong>Borra la fecha de entrega si la habia, y es correcto.</strong> La
     * cuarta rama de {@code chk_accounting_exports_lifecycle} no impone ninguna
     * condicion a {@code SUPERSEDED} justamente porque puede llegar desde cualquier
     * estado; dejar ahi un {@code delivered_at} de un fichero que ya no vale seria
     * afirmar que se entrego algo que se sustituyo.
     */
    public AccountingExport supersede() {
        if (status == AccountingExportStatus.SUPERSEDED)
            throw new AccountingExportAlreadyResolvedException(id, status);
        return new AccountingExport(id, periodKey, exportKind, attemptNumber,
                AccountingExportStatus.SUPERSEDED, generatedAt, generatedBySystemUserId, totalDebit,
                totalCredit, totalsHash, fileRef, null, null, null, createdDate, version);
    }

    /** {@code true} si ocupa el hueco de {@code uq_accounting_exports_current}. */
    public boolean isCurrent() {
        return status.occupiesTheCurrentSlot();
    }

    private void requirePending() {
        if (status != AccountingExportStatus.GENERATED)
            throw new AccountingExportAlreadyResolvedException(id, status);
    }

    private static void validate(String periodKey, AccountingExportKind exportKind,
            int attemptNumber, AccountingExportStatus status, LocalDateTime generatedAt,
            Long generatedBySystemUserId, BigDecimal totalDebit, BigDecimal totalCredit,
            String totalsHash, String fileRef, LocalDateTime deliveredAt, LocalDateTime rejectedAt,
            String rejectionReason, LocalDateTime createdDate) {
        validatePeriodKey(periodKey);
        if (exportKind == null)
            throw new IllegalArgumentException("exportKind is required");
        if (attemptNumber < 1)
            throw new IllegalArgumentException("attemptNumber must be 1 or greater");
        if (status == null)
            throw new IllegalArgumentException("status is required");
        if (generatedAt == null)
            throw new IllegalArgumentException("generatedAt is required");
        if (generatedBySystemUserId == null)
            throw new IllegalArgumentException("generatedBySystemUserId is required");
        validateBalance(totalDebit, totalCredit);
        validateHash(totalsHash);
        validateFileRef(fileRef);
        validateLifecycle(status, generatedAt, deliveredAt, rejectedAt, rejectionReason);
        if (createdDate == null)
            throw new IllegalArgumentException("createdDate is required");
    }

    private static void validatePeriodKey(String periodKey) {
        if (periodKey == null || periodKey.isBlank())
            throw new IllegalArgumentException("periodKey is required");
        if (!MONTHLY_KEY.matcher(periodKey).matches())
            throw new IllegalArgumentException(
                    "periodKey must have the form yyyy-MM: " + periodKey);
    }

    /**
     * Espejo de {@code chk_accounting_exports_balanced} mas la escala, que la
     * constraint no puede expresar. La comparacion es {@code compareTo} y no
     * {@code equals}: {@code equals} sobre {@code BigDecimal} distingue
     * {@code 10.0} de {@code 10.00}, y dos totales que cuadran perfectamente
     * quedarian rechazados por una diferencia de escala que MySQL ni siquiera ve.
     */
    private static void validateBalance(BigDecimal totalDebit, BigDecimal totalCredit) {
        if (totalDebit == null || totalCredit == null)
            throw new IllegalArgumentException("totalDebit and totalCredit are required");
        if (totalDebit.scale() > MAX_AMOUNT_SCALE || totalCredit.scale() > MAX_AMOUNT_SCALE)
            throw new IllegalArgumentException("totals must have 2 decimals or fewer");
        if (totalDebit.signum() < 0)
            throw new IllegalArgumentException("totalDebit must not be negative");
        if (totalDebit.compareTo(totalCredit) != 0)
            throw new IllegalArgumentException(
                    "the export does not balance: " + totalDebit + " debit vs " + totalCredit
                            + " credit; double entry is checked before exporting, not after");
    }

    /** Espejo de {@code chk_accounting_exports_hash}. */
    private static void validateHash(String totalsHash) {
        if (totalsHash == null || !SHA256_HEX.matcher(totalsHash).matches())
            throw new IllegalArgumentException(
                    "totalsHash must be 64 lowercase hex characters (SHA-256)");
    }

    private static void validateFileRef(String fileRef) {
        if (fileRef == null || fileRef.isBlank())
            throw new IllegalArgumentException("fileRef is required");
        if (fileRef.length() > MAX_FILE_REF_LENGTH)
            throw new IllegalArgumentException("fileRef must be 255 chars or less");
    }

    /**
     * Espejo de {@code chk_accounting_exports_lifecycle}, con las <b>cuatro</b>
     * ramas escritas. Comprobar solo que un {@code REJECTED} lleva motivo dejaria
     * entrar un {@code GENERATED} con fecha de entrega, que es una fila que se lee
     * perfecta y afirma dos cosas incompatibles.
     */
    private static void validateLifecycle(AccountingExportStatus status, LocalDateTime generatedAt,
            LocalDateTime deliveredAt, LocalDateTime rejectedAt, String rejectionReason) {
        if (rejectionReason != null && rejectionReason.length() > MAX_REJECTION_REASON_LENGTH)
            throw new IllegalArgumentException("rejectionReason must be 255 chars or less");
        switch (status) {
            case GENERATED -> requireEmpty(deliveredAt, rejectedAt, rejectionReason);
            case DELIVERED -> {
                if (deliveredAt == null || rejectedAt != null || rejectionReason != null)
                    throw new IllegalArgumentException(
                            "a DELIVERED export needs deliveredAt and nothing else");
                if (deliveredAt.isBefore(generatedAt))
                    throw new IllegalArgumentException(
                            "deliveredAt must not be before generatedAt");
            }
            case REJECTED -> {
                if (rejectedAt == null || rejectionReason == null || rejectionReason.isBlank())
                    throw new IllegalArgumentException(
                            "a REJECTED export needs rejectedAt and rejectionReason");
                if (deliveredAt != null)
                    throw new IllegalArgumentException(
                            "a REJECTED export must not keep a delivery date");
                if (rejectedAt.isBefore(generatedAt))
                    throw new IllegalArgumentException("rejectedAt must not be before generatedAt");
            }
            case SUPERSEDED -> requireEmpty(deliveredAt, rejectedAt, rejectionReason);
        }
    }

    private static void requireEmpty(LocalDateTime deliveredAt, LocalDateTime rejectedAt,
            String rejectionReason) {
        if (deliveredAt != null || rejectedAt != null || rejectionReason != null)
            throw new IllegalArgumentException(
                    "an export without an outcome must not carry delivery or rejection data");
    }

    public Long getId() {
        return id;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public AccountingExportKind getExportKind() {
        return exportKind;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AccountingExportStatus getStatus() {
        return status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public Long getGeneratedBySystemUserId() {
        return generatedBySystemUserId;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public String getTotalsHash() {
        return totalsHash;
    }

    public String getFileRef() {
        return fileRef;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
