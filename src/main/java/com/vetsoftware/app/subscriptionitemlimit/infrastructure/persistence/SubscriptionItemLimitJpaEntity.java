package com.vetsoftware.app.subscriptionitemlimit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLRestriction;

/**
 * Fila del techo congelado al firmar.
 *
 * <p>
 * Lleva {@code version} porque la fila <em>sí</em> se actualiza después de
 * nacer: las mejoras del cupo de fábrica se propagan a los contratos vivos
 * (D-75). No es un documento inmutable, y por eso dos operadores que la toquen
 * a la vez tienen que chocar en vez de pisarse.
 *
 * <p>
 * Las claves foráneas van como columnas planas: las de la base son
 * <strong>compuestas</strong> con {@code company_id} —el techo de una clínica
 * no puede colgar de la línea de otra— y una clave compuesta que comparte
 * columna con otra asociación no se mapea limpio en JPA.
 */
@Entity
@Table(name = "subscription_item_limits")
@SQLRestriction("enabled = true")
public class SubscriptionItemLimitJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "subscription_item_id", nullable = false)
    private Long subscriptionItemId;

    @Column(name = "limit_dimension_id", nullable = false)
    private Long limitDimensionId;

    @Column(name = "measure_kind", nullable = false, length = 20)
    private String measureKind;

    @Column(name = "mode", nullable = false, length = 15)
    private String mode;

    @Column(name = "limit_quantity")
    private Integer limitQuantity;

    @Column(name = "reset_period", length = 10)
    private String resetPeriod;

    @Column(name = "enforcement", nullable = false, length = 15)
    private String enforcement;

    @Column(name = "overage_unit_amount", precision = 19, scale = 2)
    private BigDecimal overageUnitAmount;

    /** {@code TINYINT UNSIGNED} en la base: {@code byte} es el mapeo exacto. */
    @Column(name = "warn_threshold", nullable = false)
    private byte warnThreshold;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SubscriptionItemLimitJpaEntity() {
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

    public Long getSubscriptionItemId() {
        return subscriptionItemId;
    }

    public void setSubscriptionItemId(Long subscriptionItemId) {
        this.subscriptionItemId = subscriptionItemId;
    }

    public Long getLimitDimensionId() {
        return limitDimensionId;
    }

    public void setLimitDimensionId(Long limitDimensionId) {
        this.limitDimensionId = limitDimensionId;
    }

    public String getMeasureKind() {
        return measureKind;
    }

    public void setMeasureKind(String measureKind) {
        this.measureKind = measureKind;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getLimitQuantity() {
        return limitQuantity;
    }

    public void setLimitQuantity(Integer limitQuantity) {
        this.limitQuantity = limitQuantity;
    }

    public String getResetPeriod() {
        return resetPeriod;
    }

    public void setResetPeriod(String resetPeriod) {
        this.resetPeriod = resetPeriod;
    }

    public String getEnforcement() {
        return enforcement;
    }

    public void setEnforcement(String enforcement) {
        this.enforcement = enforcement;
    }

    public BigDecimal getOverageUnitAmount() {
        return overageUnitAmount;
    }

    public void setOverageUnitAmount(BigDecimal overageUnitAmount) {
        this.overageUnitAmount = overageUnitAmount;
    }

    public byte getWarnThreshold() {
        return warnThreshold;
    }

    public void setWarnThreshold(byte warnThreshold) {
        this.warnThreshold = warnThreshold;
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
