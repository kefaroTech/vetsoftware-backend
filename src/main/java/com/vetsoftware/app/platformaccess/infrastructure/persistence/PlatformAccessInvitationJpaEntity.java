package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Fila de {@code platform_access_invitations}.
 *
 * <p>
 * <b>Exenta de bloqueo optimista con el código {@code E3_TOKEN}</b> y la
 * exención está escrita en la lista de {@code HexagonalArchitectureTest}:
 * encaja literalmente en «se emite, se consume una vez y caduca; nadie lo
 * edita». Su único cambio es el consumo, y ese va por un {@code UPDATE}
 * condicional cuyo {@code WHERE} es la invariante, respaldado por el índice
 * único sobre la columna generada {@code consumed_request_id}.
 *
 * <p>
 * <b>{@code accessRequestId} y {@code systemUserId} son {@code Long} planos, no
 * asociaciones.</b> Las FK siguen en la base igual. Con un {@code @ManyToOne}
 * el paquete de esta feature referenciaría una entidad de otra y el análisis
 * transitivo de las reglas de tenencia tendría cinco saltos por delante para
 * buscar {@code companies}: hoy no llegaría, pero eso es una propiedad de hoy y
 * bastaría que alguien colgase una relación de la entidad vecina para encender
 * esta feature entera sin haberla tocado. Con un {@code Long} no hay arista que
 * seguir.
 *
 * <p>
 * {@code consumed_request_id} —la columna generada {@code STORED} que emula el
 * índice único parcial— <b>no se mapea a propósito</b>: la calcula la base y
 * {@code ddl-auto: validate} no falla por columnas de más. Lo mismo con
 * {@code version}, que existe en la tabla para que ponerle bloqueo optimista
 * algún día no requiera un {@code ALTER} sobre datos.
 */
@Entity
@Table(name = "platform_access_invitations")
public class PlatformAccessInvitationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_request_id", nullable = false)
    private Long accessRequestId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "system_user_id")
    private Long systemUserId;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    protected PlatformAccessInvitationJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccessRequestId() {
        return accessRequestId;
    }

    public void setAccessRequestId(Long accessRequestId) {
        this.accessRequestId = accessRequestId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Long getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Long systemUserId) {
        this.systemUserId = systemUserId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
