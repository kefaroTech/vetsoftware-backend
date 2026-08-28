package com.vetsoftware.app.legaldocumentversion.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Una version publicada de un texto legal: los terminos, la politica de
 * privacidad, el anexo de tratamiento de datos.
 *
 * <p>
 * <strong>Es inmutable, y el dominio no ofrece la operacion que la base va a
 * rechazar.</strong> El changeset 353 instala {@code trg_ldv_bu_immutable}, un
 * disparador {@code BEFORE UPDATE} que aborta cualquier cambio de {@code code},
 * {@code document_version}, {@code kind}, {@code content},
 * {@code content_hash}, {@code published_at} o
 * {@code published_by_system_user_id}. Ofrecer aqui un {@code update(...)}
 * produciria un caso de uso que compila, pasa la revision y muere en produccion
 * con un {@code SIGNAL SQLSTATE '45000'} que el usuario lee como un 500. Por
 * eso <b>no hay setters, no hay {@code update}</b>, y la unica mutacion
 * permitida es {@link #supersede(LocalDateTime)} —que solo mueve
 * {@code superseded_at}, una de las dos columnas que el disparador deja pasar—.
 * <b>Un texto legal no se edita: se sucede.</b>
 *
 * <p>
 * <strong>La huella la calcula esta clase, no la recibe.</strong> Si el hash
 * viniera de fuera, huella y texto podrian divergir desde el primer INSERT y la
 * columna dejaria de probar nada; calculandola aqui, sobre el mismo
 * {@code content} que se va a guardar, son la misma cosa por construccion. Esa
 * huella es lo que permite que una aceptacion demuestre <em>que</em> texto se
 * acepto: la aceptacion guarda el hash, y el cliente puede volver a pedir el
 * documento por el.
 *
 * <p>
 * <strong>La columna de negocio se llama {@code documentVersion}</strong>
 * porque {@code version} esta ocupada por el bloqueo optimista. Son dos cosas
 * distintas que en el documento original compartian nombre y no caben en la
 * misma tabla.
 */
public class LegalDocumentVersion {

    private static final int MAX_CODE = 50;
    private static final int MAX_TITLE = 200;
    private static final String ALGORITMO_DE_HUELLA = "SHA-256";

    private final Long id;
    private final String code;
    private final int documentVersion;
    private final LegalDocumentKind kind;
    private final String title;
    private final String content;
    private final String contentHash;
    private final LocalDateTime publishedAt;
    private final Long publishedBySystemUserId;
    private final LocalDate effectiveFrom;
    private LocalDateTime supersededAt;
    private final LocalDateTime createdDate;
    private Long version;

    public LegalDocumentVersion(Long id, String code, int documentVersion, LegalDocumentKind kind,
            String title, String content, String contentHash, LocalDateTime publishedAt,
            Long publishedBySystemUserId, LocalDate effectiveFrom, LocalDateTime supersededAt,
            LocalDateTime createdDate, Long version) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (code.length() > MAX_CODE) {
            throw new IllegalArgumentException("code must be " + MAX_CODE + " chars or less");
        }
        if (documentVersion < 1) {
            throw new IllegalArgumentException("documentVersion must be at least 1");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (title.length() > MAX_TITLE) {
            throw new IllegalArgumentException("title must be " + MAX_TITLE + " chars or less");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (contentHash == null || !contentHash.matches("^[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "contentHash must be 64 lowercase hex chars (chk_ldv_hash)");
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("publishedAt is required");
        }
        if (publishedBySystemUserId == null) {
            throw new IllegalArgumentException("publishedBySystemUserId is required");
        }
        if (effectiveFrom == null) {
            throw new IllegalArgumentException("effectiveFrom is required");
        }
        if (supersededAt != null && supersededAt.isBefore(publishedAt)) {
            throw new IllegalArgumentException(
                    "supersededAt cannot be earlier than publishedAt (chk_ldv_supersede)");
        }
        this.id = id;
        this.code = code;
        this.documentVersion = documentVersion;
        this.kind = kind;
        this.title = title;
        this.content = content;
        this.contentHash = contentHash;
        this.publishedAt = publishedAt;
        this.publishedBySystemUserId = publishedBySystemUserId;
        this.effectiveFrom = effectiveFrom;
        this.supersededAt = supersededAt;
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Publica una version nueva. La huella se deriva del contenido aqui mismo: no
     * hay ningun camino por el que un texto entre con la huella de otro.
     */
    public static LegalDocumentVersion publish(String code, int documentVersion,
            LegalDocumentKind kind, String title, String content, Long publishedBySystemUserId,
            LocalDate effectiveFrom, LocalDateTime publishedAt, LocalDateTime createdDate) {
        return new LegalDocumentVersion(null, code, documentVersion, kind, title, content,
                hashOf(content), publishedAt, publishedBySystemUserId, effectiveFrom, null,
                createdDate, null);
    }

    /**
     * SHA-256 del contenido en UTF-8, en hexadecimal minuscula, tal como exige
     * {@code chk_ldv_hash}.
     */
    public static String hashOf(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_DE_HUELLA);
            return HexFormat.of()
                    .formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 es obligatorio en toda JVM; si falta, el entorno esta roto.
            throw new IllegalStateException("SHA-256 is not available in this JVM", exception);
        }
    }

    /**
     * Cierra la vigencia de esta version porque otra la sucede.
     *
     * <p>
     * Es la unica mutacion que existe, y mueve una sola columna. La lleva
     * {@code @Version} en su entidad JPA porque dos publicaciones simultaneas del
     * mismo documento sucederian ambas a la misma version vigente.
     */
    public void supersede(LocalDateTime supersededAt) {
        if (this.supersededAt != null) {
            throw new LegalDocumentVersionAlreadySupersededException(code, documentVersion);
        }
        if (supersededAt == null) {
            throw new IllegalArgumentException("supersededAt is required");
        }
        if (supersededAt.isBefore(publishedAt)) {
            throw new IllegalArgumentException(
                    "supersededAt cannot be earlier than publishedAt (chk_ldv_supersede)");
        }
        this.supersededAt = supersededAt;
    }

    /**
     * {@code true} mientras nadie la haya sucedido. Espejo de
     * {@code uq_ldv_current}.
     */
    public boolean isCurrent() {
        return supersededAt == null;
    }

    /** {@code true} si este es exactamente el texto que esa huella identifica. */
    public boolean matchesHash(String hash) {
        return contentHash.equals(hash);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    /** La version <em>de negocio</em>, no la de concurrencia. */
    public int getDocumentVersion() {
        return documentVersion;
    }

    public LegalDocumentKind getKind() {
        return kind;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getPublishedBySystemUserId() {
        return publishedBySystemUserId;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getSupersededAt() {
        return supersededAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
