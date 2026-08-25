package com.vetsoftware.app.platformaccess.domain;

import java.time.LocalDateTime;

/**
 * Invitación emitida tras aprobar una solicitud: token de un solo uso que
 * permite a quien pidió el acceso fijar su contraseña y nacer como
 * superadministrador.
 *
 * <p>
 * <b>No duplica el correo.</b> El {@code GET /platform/invitation/validate}
 * devuelve el correo y lo resuelve por la clave primaria de la solicitud a la
 * que apunta. Un segundo sitio donde guardar el correo es un segundo sitio
 * donde puede acabar siendo otro, y de ahí sale la identidad de la cuenta que
 * nace.
 *
 * <p>
 * Es 1:0..N con la solicitud: reemitir una invitación caducada es un
 * {@code INSERT}, no una reescritura del {@code token_hash} de la fila —eso
 * destruiría el registro de qué token se envió antes—. Que como mucho una
 * llegue a consumirse lo garantiza el índice único sobre la columna generada
 * {@code consumed_request_id}, no una lectura previa en Java, que la
 * concurrencia se come.
 */
public class PlatformAccessInvitation {

    private final Long id;
    private final Long accessRequestId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime consumedAt;
    private final Long systemUserId;
    private final LocalDateTime createdDate;

    public PlatformAccessInvitation(Long id, Long accessRequestId, String tokenHash,
            LocalDateTime expiresAt, LocalDateTime consumedAt, Long systemUserId,
            LocalDateTime createdDate) {
        if (accessRequestId == null) {
            throw new IllegalArgumentException("accessRequestId is required");
        }
        if (tokenHash == null || tokenHash.length() != PlatformAccessRequest.TOKEN_HASH_LENGTH) {
            throw new IllegalArgumentException("tokenHash must be a 64 char hex digest");
        }
        if (createdDate == null) {
            throw new IllegalArgumentException("createdDate is required");
        }
        if (expiresAt == null || !expiresAt.isAfter(createdDate)) {
            throw new IllegalArgumentException("expiresAt must be after createdDate");
        }
        if ((consumedAt == null) != (systemUserId == null)) {
            throw new IllegalArgumentException("consumedAt and systemUserId must be set together");
        }
        if (consumedAt != null && consumedAt.isBefore(createdDate)) {
            throw new IllegalArgumentException("consumedAt cannot precede createdDate");
        }
        this.id = id;
        this.accessRequestId = accessRequestId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.systemUserId = systemUserId;
        this.createdDate = createdDate;
    }

    public static PlatformAccessInvitation issue(Long accessRequestId, String tokenHash,
            LocalDateTime createdDate, LocalDateTime expiresAt) {
        return new PlatformAccessInvitation(null, accessRequestId, tokenHash, expiresAt, null, null,
                createdDate);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    /** No consumida y no caducada en este instante. No muta. */
    public boolean isUsable(LocalDateTime now) {
        return consumedAt == null && !now.isAfter(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public Long getAccessRequestId() {
        return accessRequestId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public Long getSystemUserId() {
        return systemUserId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
