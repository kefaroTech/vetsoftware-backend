package com.vetsoftware.app.paymentreversal.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * La reversion, <strong>que no es una devolucion</strong>.
 *
 * <p>
 * {@code payment_refunds} registra el dinero que salio; esta fila registra
 * <strong>el derecho que obligo a sacarlo</strong>, que es lo que hay que poder
 * ensenar. Son dos hechos distintos y confundirlos deja el expediente sin la
 * mitad que se defiende ante un tercero.
 *
 * <p>
 * <strong>Las invariantes de aqui son espejo de los once {@code CHECK} del
 * changeset 322, y en varios sitios son mas estrictas que el DDL.</strong> No
 * por gusto: MySQL rechaza una fila cuando el {@code CHECK} evalua a
 * {@code FALSE}, pero <strong>si evalua a {@code NULL} la acepta</strong>, y
 * una pertenencia a lista con el operando vacio vale {@code NULL}, no
 * {@code FALSE}. Esa trampa obligo a partir varias reglas del changeset en dos
 * mitades para que restringieran de verdad. En Java no existe: una comparacion
 * con {@code null} no se evapora, se escribe y se comprueba. Asi que aqui las
 * reglas van directas y completas, y el DDL queda como segunda barrera y no
 * como unica.
 *
 * <p>
 * <strong>Tres fechas, no una</strong>, y cada una responde a algo distinto:
 * <ul>
 * <li>{@link #getConsumerBecameAwareAt()} — cuando el cliente <em>tuvo
 * conocimiento</em>. Es donde arranca su reloj, no donde arranca el nuestro.
 * <li>{@link #getClaimReceivedAt()} — cuando <em>llego la queja</em>.
 * <li>{@link #getIssuerNotifiedAt()} — cuando se <em>notifico al emisor</em> de
 * su medio de pago, tramite que la norma le exige al consumidor.
 * </ul>
 * Sin las dos primeras no se puede alegar que reclamo fuera de plazo, que es
 * una de las pocas defensas que hay. Guardar solo la fecha de registro las
 * funde en una y borra la defensa.
 */
public class PaymentReversalRequest {

    /** Longitud de las columnas de constancia documental del changeset 322. */
    private static final int MAX_EVIDENCE_REF_LENGTH = 255;

    private final Long id;
    private final Long companyId;
    private final Long paymentId;

    private final ReversalOrigin origin;
    private final ReversalCausal causal;
    private final ConsumerDetermination consumerDetermination;

    private final LocalDateTime consumerBecameAwareAt;
    private final LocalDateTime claimReceivedAt;
    private final LocalDateTime issuerNotifiedAt;
    private final String claimEvidenceRef;

    private String acknowledgementRef;
    private LocalDateTime acknowledgedAt;

    private OppositionGround oppositionGround;
    private String oppositionEvidenceRef;
    private LocalDateTime opposedAt;

    private final LocalDateTime deadlineAt;

    private BigDecimal appliedAmount;
    private ReversalOutcome outcome;
    private Long resultingRefundId;

    private final LocalDateTime createdDate;
    private final Long version;

    public PaymentReversalRequest(Long id, Long companyId, Long paymentId, ReversalOrigin origin,
            ReversalCausal causal, ConsumerDetermination consumerDetermination,
            LocalDateTime consumerBecameAwareAt, LocalDateTime claimReceivedAt,
            LocalDateTime issuerNotifiedAt, String claimEvidenceRef, String acknowledgementRef,
            LocalDateTime acknowledgedAt, OppositionGround oppositionGround,
            String oppositionEvidenceRef, LocalDateTime opposedAt, LocalDateTime deadlineAt,
            BigDecimal appliedAmount, ReversalOutcome outcome, Long resultingRefundId,
            LocalDateTime createdDate, Long version) {
        validateIdentity(companyId, paymentId);
        validateOriginAndCausal(origin, causal, claimEvidenceRef, consumerDetermination);
        validateDates(consumerBecameAwareAt, claimReceivedAt, deadlineAt);
        validateAcknowledgement(acknowledgementRef, acknowledgedAt);
        validateOpposition(oppositionGround, oppositionEvidenceRef, opposedAt);
        validateOutcome(outcome, appliedAmount, resultingRefundId);
        this.id = id;
        this.companyId = companyId;
        this.paymentId = paymentId;
        this.origin = origin;
        this.causal = causal;
        this.consumerDetermination = consumerDetermination;
        this.consumerBecameAwareAt = consumerBecameAwareAt;
        this.claimReceivedAt = claimReceivedAt;
        this.issuerNotifiedAt = issuerNotifiedAt;
        this.claimEvidenceRef = claimEvidenceRef;
        this.acknowledgementRef = acknowledgementRef;
        this.acknowledgedAt = acknowledgedAt;
        this.oppositionGround = oppositionGround;
        this.oppositionEvidenceRef = oppositionEvidenceRef;
        this.opposedAt = opposedAt;
        this.deadlineAt = deadlineAt;
        this.appliedAmount = appliedAmount;
        this.outcome = outcome;
        this.resultingRefundId = resultingRefundId;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Expediente recien abierto: sin acuse, sin oposicion y sin desenlace.
     *
     * <p>
     * Los tres nacen vacios a proposito y no por descuido — son hechos
     * <em>posteriores</em> al nacimiento de la fila, y la regla de nulabilidad del
     * modelo es exactamente esa: admite vacio el hecho que aun no ha ocurrido. Lo
     * que vigila que el acuse llegue es el barrido de vencimientos, no un
     * {@code NOT NULL} que impediria abrir el expediente.
     */
    public static PaymentReversalRequest open(Long companyId, Long paymentId, ReversalOrigin origin,
            ReversalCausal causal, ConsumerDetermination consumerDetermination,
            LocalDateTime consumerBecameAwareAt, LocalDateTime claimReceivedAt,
            LocalDateTime issuerNotifiedAt, String claimEvidenceRef, LocalDateTime deadlineAt,
            LocalDateTime createdDate) {
        return new PaymentReversalRequest(null, companyId, paymentId, origin, causal,
                consumerDetermination, consumerBecameAwareAt, claimReceivedAt, issuerNotifiedAt,
                claimEvidenceRef, null, null, null, null, null, deadlineAt, null, null, null,
                createdDate, null);
    }

    /**
     * Deja constancia de que se le entrego al cliente el acuse de su reclamacion.
     *
     * <p>
     * Los dos datos van juntos ({@code chk_prr_acknowledgement}): una fecha sin
     * referencia no se puede exhibir y una referencia sin fecha no prueba cuando.
     */
    public void acknowledge(String reference, LocalDateTime at) {
        requireUnresolved();
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("acknowledgement reference is required");
        if (at == null)
            throw new IllegalArgumentException("acknowledgedAt is required");
        validateEvidenceLength(reference, "acknowledgement reference");
        this.acknowledgementRef = reference;
        this.acknowledgedAt = at;
    }

    /**
     * Registra la oposicion de la plataforma a la reversion.
     *
     * <p>
     * <strong>Los tres datos van juntos o no va ninguno</strong>
     * ({@code chk_prr_opposition}): una oposicion sin constancia no es una
     * oposicion, es una afirmacion propia.
     */
    public void oppose(OppositionGround ground, String evidenceRef, LocalDateTime at) {
        requireUnresolved();
        if (ground == null)
            throw new IllegalArgumentException("opposition ground is required");
        if (evidenceRef == null || evidenceRef.isBlank())
            throw new IllegalArgumentException("opposition evidence reference is required");
        if (at == null)
            throw new IllegalArgumentException("opposedAt is required");
        validateEvidenceLength(evidenceRef, "opposition evidence reference");
        this.oppositionGround = ground;
        this.oppositionEvidenceRef = evidenceRef;
        this.opposedAt = at;
    }

    /**
     * Cierra el expediente. Un desenlace que mueve dinero exige importe aplicado
     * positivo; uno que no lo mueve exige que no haya ni importe ni devolucion
     * enlazada ({@code chk_prr_applied_amount}).
     */
    public void resolve(ReversalOutcome target, BigDecimal amount, Long refundId) {
        requireUnresolved();
        if (target == null)
            throw new IllegalArgumentException("outcome is required");
        validateOutcome(target, amount, refundId);
        this.outcome = target;
        this.appliedAmount = amount;
        this.resultingRefundId = refundId;
    }

    /**
     * Un expediente sin desenlace sigue vivo y cuenta para el barrido de plazos.
     */
    public boolean isResolved() {
        return outcome != null;
    }

    /**
     * <strong>Una reversion NUNCA dispara mora, venga por donde venga.</strong>
     *
     * <p>
     * Devuelve una constante, y existe justamente por eso: el efecto que rompe el
     * modelo si nadie lo preve es que hoy una reversion entraria al proceso de
     * cobranza como un pago fallido —el saldo vuelve a subir y el reloj de la mora
     * arranca <em>contra alguien que ejercio un derecho</em>—. Escrito como metodo
     * del dominio, la regla tiene un sitio al que apuntar, un nombre que sale en
     * las llamadas y algo que un test puede fijar; escrita como comentario, se
     * pierde en la primera integracion con {@code dunning}.
     *
     * <p>
     * Vale para las <strong>dos</strong> ramas de {@link ReversalOrigin}: que la
     * pasarela notifique un contracargo sin queja previa no convierte al cliente en
     * moroso.
     */
    public boolean triggersDunning() {
        return false;
    }

    private void requireUnresolved() {
        if (outcome != null)
            throw new ReversalRequestAlreadyResolvedException(id, outcome);
    }

    private static void validateIdentity(Long companyId, Long paymentId) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (paymentId == null)
            throw new IllegalArgumentException("paymentId is required");
    }

    /**
     * {@code chk_prr_causal_required} y {@code chk_prr_claim_evidence}, escritas
     * sin la trampa del {@code NULL} que obligo a partirlas en el DDL.
     */
    private static void validateOriginAndCausal(ReversalOrigin origin, ReversalCausal causal,
            String claimEvidenceRef, ConsumerDetermination consumerDetermination) {
        if (origin == null)
            throw new IllegalArgumentException("origin is required");
        if (consumerDetermination == null)
            throw new IllegalArgumentException("consumerDetermination is required");
        if (origin != ReversalOrigin.GATEWAY_CHARGEBACK && causal == null)
            throw new IllegalArgumentException(
                    "causal is required unless the origin is a gateway chargeback");
        if (origin != ReversalOrigin.GATEWAY_CHARGEBACK
                && (claimEvidenceRef == null || claimEvidenceRef.isBlank()))
            throw new IllegalArgumentException(
                    "claim evidence reference is required unless the origin is a gateway"
                            + " chargeback");
        validateEvidenceLength(claimEvidenceRef, "claim evidence reference");
    }

    /**
     * {@code chk_prr_awareness_order} y {@code chk_prr_deadline}. El plazo se
     * guarda como dato y no como formula para que se pueda <em>listar</em> lo que
     * esta a punto de vencer: un plazo que solo existe como calculo no se puede
     * consultar.
     */
    private static void validateDates(LocalDateTime consumerBecameAwareAt,
            LocalDateTime claimReceivedAt, LocalDateTime deadlineAt) {
        if (claimReceivedAt == null)
            throw new IllegalArgumentException("claimReceivedAt is required");
        if (deadlineAt == null)
            throw new IllegalArgumentException("deadlineAt is required");
        if (consumerBecameAwareAt != null && consumerBecameAwareAt.isAfter(claimReceivedAt))
            throw new IllegalArgumentException(
                    "consumerBecameAwareAt cannot be later than claimReceivedAt");
        if (!deadlineAt.isAfter(claimReceivedAt))
            throw new IllegalArgumentException("deadlineAt must be later than claimReceivedAt");
    }

    private static void validateAcknowledgement(String acknowledgementRef,
            LocalDateTime acknowledgedAt) {
        if ((acknowledgementRef == null) != (acknowledgedAt == null))
            throw new IllegalArgumentException(
                    "acknowledgementRef and acknowledgedAt must be both present or both absent");
        validateEvidenceLength(acknowledgementRef, "acknowledgement reference");
    }

    private static void validateOpposition(OppositionGround ground, String evidenceRef,
            LocalDateTime opposedAt) {
        boolean any = ground != null || evidenceRef != null || opposedAt != null;
        boolean all = ground != null && evidenceRef != null && opposedAt != null;
        if (any && !all)
            throw new IllegalArgumentException(
                    "opposition ground, evidence reference and date must be all present or all"
                            + " absent");
        validateEvidenceLength(evidenceRef, "opposition evidence reference");
    }

    private static void validateOutcome(ReversalOutcome outcome, BigDecimal appliedAmount,
            Long resultingRefundId) {
        if (outcome == null) {
            if (appliedAmount != null || resultingRefundId != null)
                throw new IllegalArgumentException(
                        "an unresolved request cannot carry an applied amount or a refund");
            return;
        }
        if (outcome.movesMoney()) {
            if (appliedAmount == null)
                throw new IllegalArgumentException(
                        "appliedAmount is required when the outcome moves money");
            if (appliedAmount.signum() <= 0)
                throw new IllegalArgumentException("appliedAmount must be greater than zero");
            return;
        }
        if (appliedAmount != null || resultingRefundId != null)
            throw new IllegalArgumentException(
                    "a rejected or withdrawn request cannot carry an applied amount or a refund");
    }

    private static void validateEvidenceLength(String value, String label) {
        if (value != null && value.length() > MAX_EVIDENCE_REF_LENGTH)
            throw new IllegalArgumentException(label + " must be 255 chars or less");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public ReversalOrigin getOrigin() {
        return origin;
    }

    public ReversalCausal getCausal() {
        return causal;
    }

    public ConsumerDetermination getConsumerDetermination() {
        return consumerDetermination;
    }

    public LocalDateTime getConsumerBecameAwareAt() {
        return consumerBecameAwareAt;
    }

    public LocalDateTime getClaimReceivedAt() {
        return claimReceivedAt;
    }

    public LocalDateTime getIssuerNotifiedAt() {
        return issuerNotifiedAt;
    }

    public String getClaimEvidenceRef() {
        return claimEvidenceRef;
    }

    public String getAcknowledgementRef() {
        return acknowledgementRef;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public OppositionGround getOppositionGround() {
        return oppositionGround;
    }

    public String getOppositionEvidenceRef() {
        return oppositionEvidenceRef;
    }

    public LocalDateTime getOpposedAt() {
        return opposedAt;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public ReversalOutcome getOutcome() {
        return outcome;
    }

    public Long getResultingRefundId() {
        return resultingRefundId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
