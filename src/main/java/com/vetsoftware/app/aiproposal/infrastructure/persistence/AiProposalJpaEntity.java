package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Espejo de {@code ai_proposals} (changeset 383).
 *
 * <p>
 * ⛔ <strong>Ninguna asociacion a {@code CompanyJpaEntity} y ningun campo
 * {@code companyId}</strong>, y es estructural.
 * {@code VetSoftwareConditions.laFeatureTieneDatosDeEmpresa} hace
 * {@code anyMatch} sobre <em>todas</em> las {@code *JpaEntity} del arbol del
 * paquete raiz, siguiendo asociaciones hasta 5 saltos: una sola entidad de
 * {@code aiproposal} con empresa encenderia las cuatro reglas duras de BE-COV
 * sobre la rodaja entera.
 *
 * <p>
 * <strong>{@code price_list_id} y {@code privacy_notice_version_id} son
 * columnas sueltas, no asociaciones</strong> -mismo criterio que
 * {@code PriceListJpaEntity.published_by_system_user_id}-. Las FK existen en el
 * esquema y no hace falta modelarlas en JPA: esta feature guarda con que tarifa
 * se cotizo y que aviso se mostro, nunca lee sus datos desde aqui. Colgar un
 * {@code @ManyToOne} traeria un grafo ajeno a una rodaja que a proposito no
 * alcanza ninguna entidad con empresa, y obligaria a un {@code @EntityGraph} en
 * cada lectura para no caer en N+1.
 *
 * <p>
 * ⛔ <strong>{@code contact_email_hash} NO se mapea.</strong> Es una columna
 * {@code GENERATED ALWAYS ... STORED}: MySQL devuelve {@code ERROR 3105} si se
 * nombra en el {@code INSERT}, aunque el valor sea {@code NULL}. La busqueda de
 * idempotencia acotada al solicitante -{@code uq_ai_proposals_idempotency
 * (contact_email_hash, idempotency_key)}- se resuelve con SQL nativo que
 * calcula el hash en el {@code WHERE}, no leyendo la columna desde Java.
 *
 * <p>
 * {@code catalog_snapshot_hash} y {@code idempotency_key} llevan
 * {@code columnDefinition} explicito porque son {@code CHAR}: sin el, Hibernate
 * espera {@code varchar} y {@code ddl-auto: validate} tumba el arranque de la
 * aplicacion entera -el mismo motivo por el que {@code PriceListJpaEntity}
 * declara {@code char(3)} para la moneda-.
 *
 * <p>
 * El {@code @SQLDelete} lleva {@code AND version = ?} porque la entidad esta
 * versionada: en cuanto hay {@code @Version}, Hibernate liga DOS parametros al
 * SQL -primero el id, despues la version- y un {@code WHERE} de un solo
 * parametro se rompe en ejecucion sin que el compilador diga nada
 * ({@code BORRADO_LOGICO_RESPETA_LA_VERSION}).
 */
@Entity
@Table(name = "ai_proposals")
@SQLDelete(sql = "UPDATE ai_proposals SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class AiProposalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_token", nullable = false, length = 43, unique = true)
    private String publicToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProposalStatus status;

    @Column(name = "price_list_id", nullable = false)
    private Long priceListId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private ProposalBillingCycle billingCycle;

    @Column(name = "catalog_snapshot_hash", nullable = false, columnDefinition = "char(64)")
    private String catalogSnapshotHash;

    @Column(name = "privacy_notice_version_id", nullable = false)
    private Long privacyNoticeVersionId;

    @Column(name = "idempotency_key", columnDefinition = "char(36)")
    private String idempotencyKey;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "turn_count", nullable = false)
    private int turnCount;

    @Column(name = "total_input_tokens", nullable = false)
    private int totalInputTokens;

    @Column(name = "total_output_tokens", nullable = false)
    private int totalOutputTokens;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected AiProposalJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public ProposalBillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(ProposalBillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public String getCatalogSnapshotHash() {
        return catalogSnapshotHash;
    }

    public void setCatalogSnapshotHash(String catalogSnapshotHash) {
        this.catalogSnapshotHash = catalogSnapshotHash;
    }

    public Long getPrivacyNoticeVersionId() {
        return privacyNoticeVersionId;
    }

    public void setPrivacyNoticeVersionId(Long privacyNoticeVersionId) {
        this.privacyNoticeVersionId = privacyNoticeVersionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public int getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(int totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public int getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(int totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(LocalDateTime firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(LocalDateTime anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
