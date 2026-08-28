package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

/**
 * Fila de la excepción negociada.
 *
 * <p>
 * <strong>{@code alive_company_marker} no se mapea.</strong> Es una columna
 * generada {@code STORED} que la base calcula sola y que solo existe para
 * sostener el índice único sobre (marcador, eje). Mapearla haría que Hibernate
 * intentara escribirla y el {@code INSERT} moriría.
 */
@Entity
@Table(name = "company_limit_overrides")
@SQLRestriction("enabled = true")
public class CompanyLimitOverrideJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "limit_dimension_id", nullable = false)
    private Long limitDimensionId;

    @Column(name = "limit_quantity", nullable = false)
    private int limitQuantity;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /** Vacío = rige hoy. Escribirlo es cerrar el pacto. */
    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "reason_code", nullable = false, length = 30)
    private String reasonCode;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "granted_by_system_user_id", nullable = false)
    private Long grantedBySystemUserId;

    @Column(name = "revoked_by_system_user_id")
    private Long revokedBySystemUserId;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason_code", length = 30)
    private String revokedReasonCode;

    @Column(name = "revoked_reason", length = 255)
    private String revokedReason;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CompanyLimitOverrideJpaEntity() {
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

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public void setLimitDimensionId(Long limitDimensionId) {
        this.limitDimensionId = limitDimensionId;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(int limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getGrantedBySystemUserId() {
        return grantedBySystemUserId;
    }

    public void setGrantedBySystemUserId(Long grantedBySystemUserId) {
        this.grantedBySystemUserId = grantedBySystemUserId;
    }

    public Long getRevokedBySystemUserId() {
        return revokedBySystemUserId;
    }

    public void setRevokedBySystemUserId(Long revokedBySystemUserId) {
        this.revokedBySystemUserId = revokedBySystemUserId;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReasonCode() {
        return revokedReasonCode;
    }

    public void setRevokedReasonCode(String revokedReasonCode) {
        this.revokedReasonCode = revokedReasonCode;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
