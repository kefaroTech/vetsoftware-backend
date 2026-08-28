package com.vetsoftware.app.customercredit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un asiento del libro del saldo a favor.
 *
 * <p>
 * <strong>Esta clase sustituye a un acumulador, y esa es toda su razon de
 * ser.</strong> El saldo a favor era la unica tabla del bloque del dinero cuya
 * verdad dependia de que ningun camino de codigo se equivocara —un contador que
 * se lee, se suma y se guarda—. Aqui <strong>el saldo no se guarda: se
 * suma</strong>. La verdad es {@code SUM(customer_credit_entries.amount)};
 * {@link CustomerCreditBalance} es solo una proyeccion que existe para poder
 * acotar el gasto.
 *
 * <p>
 * <strong>Estrictamente de solo anadir.</strong> Todos los campos son
 * {@code final} y no hay un solo mutador, porque la tabla no lleva
 * {@code enabled} ni {@code version}: corregir un asiento es escribir otro que
 * lo compensa, y los dos quedan. Un libro que deja reescribir sus asientos no
 * es un libro.
 *
 * <p>
 * <strong>Por lotes, y se consume primero lo que antes caduca</strong> (D-71).
 * Sin lotes la caducidad no es calculable: cien mil que caducan en diciembre
 * mas cincuenta mil sin fecha, consumidos ciento veinte mil, admite dos
 * respuestas defendibles y la suma no sabe cual. Por eso todo asiento que resta
 * —{@code CONSUMPTION} y {@code EXPIRATION}— tiene que nombrar el lote del que
 * sale, y por eso {@code expiresOn} solo tiene sentido sobre un {@code GRANT}.
 *
 * <p>
 * Las invariantes de abajo son el espejo literal de los seis {@code CHECK} del
 * changeset 323. No estan duplicadas por gusto: la base rechaza la fila con un
 * error de integridad que no dice que regla se rompio, y el dominio la rechaza
 * antes con un mensaje que si lo dice.
 */
public class CustomerCreditEntry {

    /** Longitud de {@code customer_credit_entries.client_request_id}. */
    public static final int MAX_CLIENT_REQUEST_ID_LENGTH = 64;

    private final Long id;
    private final Long companyId;
    private final CreditEntryKind entryKind;
    private final BigDecimal amount;

    /** El lote del que sale este asiento. Obligatorio al restar, vacio al dar. */
    private final Long lotEntryId;

    private final CreditOriginKind originKind;
    private final Long originPaymentId;
    private final Long originDocumentId;
    private final Long originSubscriptionId;

    private final LocalDateTime occurredAt;

    /**
     * Cuando el asiento <em>cuenta</em>, que no es lo mismo que cuando se registro.
     * Es la fecha que decide en que periodo cae el hecho.
     */
    private final LocalDate valueDate;

    /** Solo sobre un {@code GRANT}: un consumo no caduca, ya se gasto. */
    private final LocalDate expiresOn;

    private final String clientRequestId;
    private final LocalDateTime createdDate;

    public CustomerCreditEntry(Long id, Long companyId, CreditEntryKind entryKind,
            BigDecimal amount, Long lotEntryId, CreditOriginKind originKind, Long originPaymentId,
            Long originDocumentId, Long originSubscriptionId, LocalDateTime occurredAt,
            LocalDate valueDate, LocalDate expiresOn, String clientRequestId,
            LocalDateTime createdDate) {
        validate(companyId, entryKind, amount, lotEntryId, originKind, originPaymentId,
                originDocumentId, originSubscriptionId, occurredAt, valueDate, expiresOn,
                clientRequestId);
        this.id = id;
        this.companyId = companyId;
        this.entryKind = entryKind;
        this.amount = amount;
        this.lotEntryId = lotEntryId;
        this.originKind = originKind;
        this.originPaymentId = originPaymentId;
        this.originDocumentId = originDocumentId;
        this.originSubscriptionId = originSubscriptionId;
        this.occurredAt = occurredAt;
        this.valueDate = valueDate;
        this.expiresOn = expiresOn;
        this.clientRequestId = clientRequestId;
        this.createdDate = createdDate;
    }

    /**
     * Alta de saldo: abre un lote. Es el unico asiento que puede llevar fecha de
     * caducidad, y el unico que no nombra un lote porque <em>es</em> el lote.
     */
    public static CustomerCreditEntry grant(Long companyId, BigDecimal amount,
            CreditOriginKind originKind, Long originPaymentId, Long originDocumentId,
            Long originSubscriptionId, LocalDateTime occurredAt, LocalDate valueDate,
            LocalDate expiresOn, String clientRequestId, LocalDateTime createdDate) {
        return new CustomerCreditEntry(null, companyId, CreditEntryKind.GRANT, amount, null,
                originKind, originPaymentId, originDocumentId, originSubscriptionId, occurredAt,
                valueDate, expiresOn, clientRequestId, createdDate);
    }

    /**
     * Consumo contra un lote concreto. El importe llega en positivo —es lo que se
     * gasta— y se guarda negado, porque el saldo es una suma y no un contador con
     * reglas aparte.
     */
    public static CustomerCreditEntry consumption(Long companyId, BigDecimal spent, Long lotEntryId,
            Long originDocumentId, LocalDateTime occurredAt, LocalDate valueDate,
            String clientRequestId, LocalDateTime createdDate) {
        return new CustomerCreditEntry(null, companyId, CreditEntryKind.CONSUMPTION, negate(spent),
                lotEntryId, CreditOriginKind.APPLICATION, null, originDocumentId, null, occurredAt,
                valueDate, null, clientRequestId, createdDate);
    }

    /**
     * Caducidad del remanente de un lote. Sin origen documental: no lo provoca
     * ningun documento, lo provoca el calendario.
     */
    public static CustomerCreditEntry expiration(Long companyId, BigDecimal remaining,
            Long lotEntryId, LocalDateTime occurredAt, LocalDate valueDate, String clientRequestId,
            LocalDateTime createdDate) {
        return new CustomerCreditEntry(null, companyId, CreditEntryKind.EXPIRATION,
                negate(remaining), lotEntryId, CreditOriginKind.EXPIRY, null, null, null,
                occurredAt, valueDate, null, clientRequestId, createdDate);
    }

    private static BigDecimal negate(BigDecimal value) {
        if (value == null)
            throw new IllegalArgumentException("amount is required");
        return value.negate();
    }

    /** Lo que este asiento le hace al saldo: su propio importe, con su signo. */
    public BigDecimal delta() {
        return amount;
    }

    private static void validate(Long companyId, CreditEntryKind entryKind, BigDecimal amount,
            Long lotEntryId, CreditOriginKind originKind, Long originPaymentId,
            Long originDocumentId, Long originSubscriptionId, LocalDateTime occurredAt,
            LocalDate valueDate, LocalDate expiresOn, String clientRequestId) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (entryKind == null)
            throw new IllegalArgumentException("entryKind is required");
        if (originKind == null)
            throw new IllegalArgumentException("originKind is required");
        validateAmount(entryKind, amount);
        validateLot(entryKind, lotEntryId);
        validateOriginBranch(originKind, originPaymentId, originDocumentId, originSubscriptionId);
        if (occurredAt == null)
            throw new IllegalArgumentException("occurredAt is required");
        if (valueDate == null)
            throw new IllegalArgumentException("valueDate is required");
        // chk_cce_expiry_only_on_grant: un consumo no caduca, ya se gasto.
        if (expiresOn != null && entryKind != CreditEntryKind.GRANT)
            throw new IllegalArgumentException("only a GRANT entry can expire");
        validateClientRequestId(clientRequestId);
    }

    /** Espejo de {@code chk_cce_amount_not_zero} y {@code chk_cce_sign}. */
    private static void validateAmount(CreditEntryKind entryKind, BigDecimal amount) {
        if (amount == null)
            throw new IllegalArgumentException("amount is required");
        if (amount.signum() == 0)
            throw new IllegalArgumentException("amount cannot be zero");
        if (entryKind == CreditEntryKind.GRANT && amount.signum() < 0)
            throw new IllegalArgumentException("a GRANT entry must be positive");
        if (entryKind.consumesLot() && amount.signum() > 0)
            throw new IllegalArgumentException("a " + entryKind + " entry must be negative");
    }

    /** Espejo de {@code chk_cce_lot}. */
    private static void validateLot(CreditEntryKind entryKind, Long lotEntryId) {
        if (entryKind.consumesLot() && lotEntryId == null)
            throw new IllegalArgumentException(
                    "a " + entryKind + " entry must name the lot it comes from");
        if (entryKind == CreditEntryKind.GRANT && lotEntryId != null)
            throw new IllegalArgumentException("a GRANT entry is the lot; it cannot name another");
    }

    /**
     * Espejo de {@code chk_cce_origin_branch}: cada rama apunta a lo suyo y a nada
     * mas. Un asiento con dos origenes no se puede atribuir a ninguno.
     */
    private static void validateOriginBranch(CreditOriginKind originKind, Long originPaymentId,
            Long originDocumentId, Long originSubscriptionId) {
        boolean payment = originPaymentId != null;
        boolean document = originDocumentId != null;
        boolean subscription = originSubscriptionId != null;
        if (originKind.pointsToPayment() && !(payment && !document && !subscription))
            throw new IllegalArgumentException(
                    "an OVERPAYMENT entry must reference only its payment");
        if (originKind.pointsToDocument() && !(document && !payment && !subscription))
            throw new IllegalArgumentException(
                    "a " + originKind + " entry must reference only its billing document");
        if (originKind.pointsToSubscription() && !(subscription && !payment && !document))
            throw new IllegalArgumentException(
                    "a CANCELLATION entry must reference only its subscription");
        if (originKind.pointsToNothing() && (payment || document || subscription))
            throw new IllegalArgumentException(
                    "a " + originKind + " entry cannot reference any origin document");
    }

    /**
     * Obligatoria, y no es simetria con las demas tablas: un libro de dinero sin
     * llave de idempotencia es un doble clic esperando. Aqui la columna es
     * {@code NOT NULL} justamente por eso.
     */
    private static void validateClientRequestId(String clientRequestId) {
        if (clientRequestId == null || clientRequestId.isBlank())
            throw new IllegalArgumentException("clientRequestId is required");
        if (clientRequestId.length() > MAX_CLIENT_REQUEST_ID_LENGTH)
            throw new IllegalArgumentException("clientRequestId must be 64 chars or less");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public CreditEntryKind getEntryKind() {
        return entryKind;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getLotEntryId() {
        return lotEntryId;
    }

    public CreditOriginKind getOriginKind() {
        return originKind;
    }

    public Long getOriginPaymentId() {
        return originPaymentId;
    }

    public Long getOriginDocumentId() {
        return originDocumentId;
    }

    public Long getOriginSubscriptionId() {
        return originSubscriptionId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
