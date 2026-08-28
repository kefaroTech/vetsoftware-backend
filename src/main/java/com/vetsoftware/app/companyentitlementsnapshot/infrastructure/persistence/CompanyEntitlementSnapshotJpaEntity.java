package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fila de la foto de permisos.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code version}</strong>: bitácora
 * probatoria, solo se agrega. Va declarada como exenta de bloqueo optimista con
 * código {@code E1_APPEND_ONLY} en {@code HexagonalArchitectureTest}.
 *
 * <p>
 * El documento va como {@code JSON} en la base y {@code String} en Java. Es
 * deliberado que esta rodaja no conozca la forma de los permisos: si la
 * conociera, la bitácora se rompería cada vez que esa tabla evolucionara — que
 * es lo contrario de lo que se le pide a una evidencia.
 */
@Entity
@Table(name = "company_entitlement_snapshots")
public class CompanyEntitlementSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "recalculated_at", nullable = false)
    private LocalDateTime recalculatedAt;

    @Column(name = "actor_employee_id")
    private Long actorEmployeeId;

    @Column(name = "actor_system_user_id")
    private Long actorSystemUserId;

    @Column(name = "actor_is_process", nullable = false)
    private boolean actorIsProcess;

    @Column(name = "trigger_reason", nullable = false, length = 40)
    private String triggerReason;

    @Column(name = "amendment_id")
    private Long amendmentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    /**
     * Fuera del documento y no dentro: dentro no sería ni indexable ni verificable
     * por el motor, y el día que alguien renombre una clave las consultas sobre
     * fotos viejas devolverían vacío en silencio.
     */
    @Column(name = "payload_format_version", nullable = false)
    private int payloadFormatVersion;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected CompanyEntitlementSnapshotJpaEntity() {
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

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public void setRecalculatedAt(LocalDateTime recalculatedAt) {
        this.recalculatedAt = recalculatedAt;
    }

    public Long getActorEmployeeId() {
        return actorEmployeeId;
    }

    public void setActorEmployeeId(Long actorEmployeeId) {
        this.actorEmployeeId = actorEmployeeId;
    }

    public Long getActorSystemUserId() {
        return actorSystemUserId;
    }

    public void setActorSystemUserId(Long actorSystemUserId) {
        this.actorSystemUserId = actorSystemUserId;
    }

    public boolean isActorIsProcess() {
        return actorIsProcess;
    }

    public void setActorIsProcess(boolean actorIsProcess) {
        this.actorIsProcess = actorIsProcess;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public Long getAmendmentId() {
        return amendmentId;
    }

    public void setAmendmentId(Long amendmentId) {
        this.amendmentId = amendmentId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getPayloadFormatVersion() {
        return payloadFormatVersion;
    }

    public void setPayloadFormatVersion(int payloadFormatVersion) {
        this.payloadFormatVersion = payloadFormatVersion;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
