package com.vetsoftware.app.unitmeasure.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Código de unidad admitido por el contrato fiscal. Es un catálogo global, no dependiente de empresa. */
@Entity
@Table(name = "unit_measure_catalog")
@SQLDelete(sql = "UPDATE unit_measure_catalog SET enabled = false WHERE code = ?")
@SQLRestriction("enabled = true")
public class UnitMeasureCatalogJpaEntity {
    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String symbol;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(nullable = false)
    private boolean enabled = true;

    protected UnitMeasureCatalogJpaEntity() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
