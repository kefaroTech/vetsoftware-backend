package com.vetsoftware.app.companylimitevent.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Fila de la bitácora de cupo.
 *
 * <p>
 * <strong>Sin {@code enabled} y sin {@code version}, a propósito.</strong> Una
 * prueba que se puede desactivar no prueba nada, y aquí solo se agrega: no hay
 * dos ediciones simultáneas que puedan pisarse porque no hay ninguna edición.
 * Va declarada como exenta de bloqueo optimista con código
 * {@code E1_APPEND_ONLY} en {@code HexagonalArchitectureTest}.
 */
@Entity
@Table(name = "company_limit_events")
public class CompanyLimitEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "limit_dimension_id", nullable = false)
    private Long limitDimensionId;

    @Column(name = "event_type", nullable = false, length = 25)
    private String eventType;

    @Column(name = "limit_quantity", nullable = false)
    private int limitQuantity;

    @Column(name = "used_quantity", nullable = false)
    private int usedQuantity;

    @Column(name = "requested_delta", nullable = false)
    private int requestedDelta;

    @Column(name = "limit_source", nullable = false, length = 20)
    private String limitSource;

    @Column(name = "override_id")
    private Long overrideId;

    @Column(name = "actor_employee_id")
    private Long actorEmployeeId;

    @Column(name = "actor_system_user_id")
    private Long actorSystemUserId;

    /**
     * Sin valor por defecto a propósito, igual que en el esquema: quien escribe el
     * hecho tiene que declarar quién lo hizo.
     */
    @Column(name = "actor_is_process", nullable = false)
    private boolean actorIsProcess;

    @Column(name = "reason_code", length = 30)
    private String reasonCode;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected CompanyLimitEventJpaEntity() {
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public int getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(int limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(int usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public int getRequestedDelta() {
        return requestedDelta;
    }

    public void setRequestedDelta(int requestedDelta) {
        this.requestedDelta = requestedDelta;
    }

    public String getLimitSource() {
        return limitSource;
    }

    public void setLimitSource(String limitSource) {
        this.limitSource = limitSource;
    }

    public Long getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(Long overrideId) {
        this.overrideId = overrideId;
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

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
