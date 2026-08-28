package com.vetsoftware.app.catalogitemlimit.infrastructure.persistence;

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
 * Fila del techo de fábrica.
 *
 * <p>
 * <strong>Sin asociaciones</strong>: ni al artículo ni al eje. Es el mismo
 * slice global que el catálogo y basta con que una asociación llegue a la
 * entidad de empresas para que las cuatro reglas duras de BE-COV caigan sobre
 * la feature entera.
 *
 * <p>
 * {@code warn_threshold} es {@code byte} y no {@code int}: la columna es
 * {@code TINYINT UNSIGNED} y Hibernate mapea {@code byte} a
 * {@code Types.TINYINT} exacto. Con {@code int} la validación de esquema
 * fallaría al arrancar. El dominio lo maneja como entero entre 1 y 100 y el
 * mapper hace la conversión.
 */
@Entity
@Table(name = "catalog_item_limits")
@SQLRestriction("enabled = true")
public class CatalogItemLimitJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Column(name = "limit_dimension_id", nullable = false)
    private Long limitDimensionId;

    /**
     * Copia atada por clave foránea contra
     * {@code limit_dimensions(id, measure_kind)}.
     */
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

    @Column(name = "warn_threshold", nullable = false)
    private byte warnThreshold;

    @Column(name = "trial_mode", nullable = false, length = 15)
    private String trialMode;

    @Column(name = "trial_limit_quantity")
    private Integer trialLimitQuantity;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected CatalogItemLimitJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
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

    public String getTrialMode() {
        return trialMode;
    }

    public void setTrialMode(String trialMode) {
        this.trialMode = trialMode;
    }

    public Integer getTrialLimitQuantity() {
        return trialLimitQuantity;
    }

    public void setTrialLimitQuantity(Integer trialLimitQuantity) {
        this.trialLimitQuantity = trialLimitQuantity;
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
