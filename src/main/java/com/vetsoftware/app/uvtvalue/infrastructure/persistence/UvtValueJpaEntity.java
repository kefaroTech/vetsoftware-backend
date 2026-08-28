package com.vetsoftware.app.uvtvalue.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espejo de {@code uvt_values} (changeset 347).
 *
 * <p>
 * {@code fiscalYear} va {@code short} y no {@code int} porque la columna es
 * {@code SMALLINT}: con {@code int}, {@code ddl-auto: validate} rompe el
 * arranque con «found [smallint], but expecting [integer]». El dominio lo
 * maneja como {@code int} —un ano no es conceptualmente un dato de dos bytes— y
 * el mapper hace la conversion en un unico sitio.
 *
 * <p>
 * Sin {@code @Version}: exenta {@code E1_APPEND_ONLY}.
 */
@Entity
@Table(name = "uvt_values")
public class UvtValueJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fiscal_year", nullable = false)
    private short fiscalYear;

    @Column(name = "value_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal valueAmount;

    @Column(name = "legal_reference", nullable = false, length = 255)
    private String legalReference;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected UvtValueJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public short getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(short fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public BigDecimal getValueAmount() {
        return valueAmount;
    }

    public void setValueAmount(BigDecimal valueAmount) {
        this.valueAmount = valueAmount;
    }

    public String getLegalReference() {
        return legalReference;
    }

    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
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
}
