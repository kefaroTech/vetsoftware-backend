package com.vetsoftware.app.paymentreversal.infrastructure.persistence;

import com.vetsoftware.app.paymentreversal.domain.ConsumerDetermination;
import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import com.vetsoftware.app.paymentreversal.domain.ReversalCausal;
import com.vetsoftware.app.paymentreversal.domain.ReversalOrigin;
import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
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
import java.time.LocalDateTime;

/**
 * {@code payment_reversal_requests}.
 *
 * <p>
 * <strong>Sin una sola asociacion {@code @ManyToOne}: las claves foraneas viven
 * aqui como escalares.</strong> Las dos que salen de esta tabla
 * —{@code fk_prr_payment} y {@code fk_prr_refund}— son <em>compuestas</em>
 * {@code (company_id, id)}, y mapearlas como asociacion obliga a un
 * {@code @JoinColumns} en el que {@code company_id} tiene que ir
 * {@code insertable = false, updatable = false} porque es una unica columna
 * fisica compartida y solo un mapeo puede ser su dueno. Hibernate ademas exige
 * que todas las columnas de una propiedad compartan modo de escritura, asi que
 * la asociacion entera queda de solo lectura; y equivocarse ahi no falla en
 * esta clase sino en el {@code entityManagerFactory}, que se lleva por delante
 * la aplicacion entera sin senalar aqui. Es la trampa que documenta
 * {@code BillingDocumentApplicationJpaEntity}.
 *
 * <p>
 * Como este slice <strong>nunca navega</strong> hacia el pago ni hacia la
 * devolucion —lo que necesita del pago se lo trae
 * {@code SubscriptionPaymentQueryPort} en una consulta acotada, y de la
 * devolucion solo guarda el id— la asociacion no compraria nada y si traeria el
 * N+1 que obligaria a poner {@code @EntityGraph} en cada finder. Escalares.
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: el expediente se reescribe hasta tres
 * veces despues de nacer —acuse, oposicion y desenlace— y dos operadores
 * resolviendo a la vez se pisarian sin ruido. <strong>Sin
 * {@code enabled}</strong>, y por tanto sin {@code @SQLDelete} ni
 * {@code @SQLRestriction}: es la prueba con la que se defiende la reversion
 * ante el emisor y ante la autoridad, y una prueba que se puede desactivar no
 * prueba nada.
 */
@Entity
@Table(name = "payment_reversal_requests")
public class PaymentReversalRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 25)
    private ReversalOrigin origin;

    /** Vacia solo cuando el origen es un contracargo de la pasarela. */
    @Enumerated(EnumType.STRING)
    @Column(name = "causal", length = 25)
    private ReversalCausal causal;

    @Enumerated(EnumType.STRING)
    @Column(name = "consumer_determination", nullable = false, length = 20)
    private ConsumerDetermination consumerDetermination;

    /** Cuando el cliente tuvo conocimiento: donde arranca SU reloj. */
    @Column(name = "consumer_became_aware_at")
    private LocalDateTime consumerBecameAwareAt;

    @Column(name = "claim_received_at", nullable = false)
    private LocalDateTime claimReceivedAt;

    @Column(name = "issuer_notified_at")
    private LocalDateTime issuerNotifiedAt;

    @Column(name = "claim_evidence_ref", length = 255)
    private String claimEvidenceRef;

    @Column(name = "acknowledgement_ref", length = 255)
    private String acknowledgementRef;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "opposition_ground", length = 30)
    private OppositionGround oppositionGround;

    @Column(name = "opposition_evidence_ref", length = 255)
    private String oppositionEvidenceRef;

    @Column(name = "opposed_at")
    private LocalDateTime opposedAt;

    /**
     * El plazo como <strong>dato</strong> y no como calculo, para que se pueda
     * listar lo que esta a punto de vencer. Lo sirve
     * {@code ix_payment_reversal_requests_deadline}.
     */
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "applied_amount", precision = 19, scale = 2)
    private BigDecimal appliedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20)
    private ReversalOutcome outcome;

    @Column(name = "resulting_refund_id")
    private Long resultingRefundId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PaymentReversalRequestJpaEntity() {
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

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public ReversalOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(ReversalOrigin origin) {
        this.origin = origin;
    }

    public ReversalCausal getCausal() {
        return causal;
    }

    public void setCausal(ReversalCausal causal) {
        this.causal = causal;
    }

    public ConsumerDetermination getConsumerDetermination() {
        return consumerDetermination;
    }

    public void setConsumerDetermination(ConsumerDetermination consumerDetermination) {
        this.consumerDetermination = consumerDetermination;
    }

    public LocalDateTime getConsumerBecameAwareAt() {
        return consumerBecameAwareAt;
    }

    public void setConsumerBecameAwareAt(LocalDateTime consumerBecameAwareAt) {
        this.consumerBecameAwareAt = consumerBecameAwareAt;
    }

    public LocalDateTime getClaimReceivedAt() {
        return claimReceivedAt;
    }

    public void setClaimReceivedAt(LocalDateTime claimReceivedAt) {
        this.claimReceivedAt = claimReceivedAt;
    }

    public LocalDateTime getIssuerNotifiedAt() {
        return issuerNotifiedAt;
    }

    public void setIssuerNotifiedAt(LocalDateTime issuerNotifiedAt) {
        this.issuerNotifiedAt = issuerNotifiedAt;
    }

    public String getClaimEvidenceRef() {
        return claimEvidenceRef;
    }

    public void setClaimEvidenceRef(String claimEvidenceRef) {
        this.claimEvidenceRef = claimEvidenceRef;
    }

    public String getAcknowledgementRef() {
        return acknowledgementRef;
    }

    public void setAcknowledgementRef(String acknowledgementRef) {
        this.acknowledgementRef = acknowledgementRef;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public OppositionGround getOppositionGround() {
        return oppositionGround;
    }

    public void setOppositionGround(OppositionGround oppositionGround) {
        this.oppositionGround = oppositionGround;
    }

    public String getOppositionEvidenceRef() {
        return oppositionEvidenceRef;
    }

    public void setOppositionEvidenceRef(String oppositionEvidenceRef) {
        this.oppositionEvidenceRef = oppositionEvidenceRef;
    }

    public LocalDateTime getOpposedAt() {
        return opposedAt;
    }

    public void setOpposedAt(LocalDateTime opposedAt) {
        this.opposedAt = opposedAt;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public void setAppliedAmount(BigDecimal appliedAmount) {
        this.appliedAmount = appliedAmount;
    }

    public ReversalOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(ReversalOutcome outcome) {
        this.outcome = outcome;
    }

    public Long getResultingRefundId() {
        return resultingRefundId;
    }

    public void setResultingRefundId(Long resultingRefundId) {
        this.resultingRefundId = resultingRefundId;
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
