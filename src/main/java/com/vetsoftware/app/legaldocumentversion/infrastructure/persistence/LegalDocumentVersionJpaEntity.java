package com.vetsoftware.app.legaldocumentversion.infrastructure.persistence;

import com.vetsoftware.app.legaldocumentversion.domain.LegalDocumentKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Espejo de {@code legal_document_versions} (changeset 353).
 *
 * <p>
 * <strong>Con {@code @Version}</strong>: la vigencia se cierra sobre la propia
 * fila ({@code superseded_at}) y dos publicaciones simultaneas del mismo
 * documento sucederian ambas a la misma version vigente.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code @SQLDelete}</strong>: un texto legal
 * publicado no se desactiva ni se borra, se sucede. La columna no existe en la
 * tabla.
 *
 * <p>
 * <strong>{@code current_version_marker} no se mapea a proposito</strong>: es
 * una columna generada ({@code STORED}) que la base calcula a partir de
 * {@code superseded_at} para sostener {@code uq_ldv_current}. Mapearla haria
 * que Hibernate intentara escribirla y MySQL rechazaria el INSERT.
 *
 * <p>
 * {@code content} lleva {@code columnDefinition = "MEDIUMTEXT"} y
 * {@code contentHash} {@code char(64)}: sin ellos Hibernate espera
 * {@code varchar(255)} y {@code varchar(64)}, y {@code ddl-auto: validate}
 * rompe el arranque —el mismo motivo por el que {@code PriceListJpaEntity}
 * declara {@code char(3)} para la moneda—.
 */
@Entity
@Table(name = "legal_document_versions")
public class LegalDocumentVersionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "document_version", nullable = false)
    private int documentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private LegalDocumentKind kind;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, columnDefinition = "char(64)")
    private String contentHash;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "published_by_system_user_id", nullable = false)
    private Long publishedBySystemUserId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "superseded_at")
    private LocalDateTime supersededAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected LegalDocumentVersionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(int documentVersion) {
        this.documentVersion = documentVersion;
    }

    public LegalDocumentKind getKind() {
        return kind;
    }

    public void setKind(LegalDocumentKind kind) {
        this.kind = kind;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getPublishedBySystemUserId() {
        return publishedBySystemUserId;
    }

    public void setPublishedBySystemUserId(Long publishedBySystemUserId) {
        this.publishedBySystemUserId = publishedBySystemUserId;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getSupersededAt() {
        return supersededAt;
    }

    public void setSupersededAt(LocalDateTime supersededAt) {
        this.supersededAt = supersededAt;
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
