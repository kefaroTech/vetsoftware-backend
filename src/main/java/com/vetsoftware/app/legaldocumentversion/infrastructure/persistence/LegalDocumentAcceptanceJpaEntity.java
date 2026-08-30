package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * La evidencia de que alguien acepto un texto legal (changeset 387).
 *
 * <p>
 * <strong>Tabla propia y no una columna en {@code ai_proposals}</strong>, y se
 * argumenta porque la columna era mas barata hoy: el resto del producto ya
 * necesita aceptaciones -{@code LegalConsentCheckbox} del front devuelve el par
 * {@code (code, documentVersion)} y ni la contratacion ni el alta de empresa
 * tienen hoy donde escribirlo-, son <strong>dos</strong> documentos por
 * propuesta -la politica y la transferencia internacional del articulo 26, que
 * no caben en un {@code consent_at}- y una aceptacion se revoca, que una
 * columna no sabe representar sin inventar un segundo
 * {@code consent_revoked_at} en cada sitio que consienta.
 *
 * <p>
 * &#9940; <strong>{@code subject_ref} es un {@code VARCHAR} y no una FK
 * polimorfica.</strong> Una clave ajena que apunta a tablas distintas segun
 * otra columna es el antipatron "Polymorphic Associations" de Karwin, y aqui
 * ademas ataria la rodaja legal a {@code aiproposal}. Para una propuesta el
 * valor es su <strong>id</strong>, nunca su {@code public_token}: el token es
 * el secreto de la URL, y copiarlo a una segunda tabla lo multiplica por dos y
 * lo saca del control de acceso que lo protege.
 *
 * <p>
 * <strong>Sin {@code @Version} y sin {@code enabled}</strong>: se inserta y no
 * se corrige. La revocacion se escribe en {@code revoked_at} de la misma fila y
 * es un hecho posterior, no una edicion de lo que se acepto. Su entrada esta en
 * {@code ENTIDADES_EXENTAS_DE_VERSION} con {@code E1_APPEND_ONLY}, que es lo
 * que convierte esa ausencia en una decision escrita.
 */
@Entity
@Table(name = "legal_document_acceptances")
public class LegalDocumentAcceptanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_document_version_id", nullable = false)
    private Long legalDocumentVersionId;

    @Column(name = "subject_kind", nullable = false, length = 20)
    private String subjectKind;

    @Column(name = "subject_ref", nullable = false, length = 64)
    private String subjectRef;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "accepted_ip_hash", columnDefinition = "char(64)")
    private String acceptedIpHash;

    @Column(name = "user_agent_hash", columnDefinition = "char(64)")
    private String userAgentHash;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected LegalDocumentAcceptanceJpaEntity() {
    }

    public LegalDocumentAcceptanceJpaEntity(Long legalDocumentVersionId, String subjectKind,
            String subjectRef, LocalDateTime acceptedAt, String acceptedIpHash,
            String userAgentHash, LocalDateTime createdDate) {
        this.legalDocumentVersionId = legalDocumentVersionId;
        this.subjectKind = subjectKind;
        this.subjectRef = subjectRef;
        this.acceptedAt = acceptedAt;
        this.acceptedIpHash = acceptedIpHash;
        this.userAgentHash = userAgentHash;
        this.createdDate = createdDate;
    }

    public Long getId() {
        return id;
    }

    public Long getLegalDocumentVersionId() {
        return legalDocumentVersionId;
    }

    public String getSubjectKind() {
        return subjectKind;
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public String getAcceptedIpHash() {
        return acceptedIpHash;
    }

    public String getUserAgentHash() {
        return userAgentHash;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
