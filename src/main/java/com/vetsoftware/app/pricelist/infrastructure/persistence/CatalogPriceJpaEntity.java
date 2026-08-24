package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Tabla {@code catalog_prices}.
 *
 * <p>
 * Los importes son {@code DECIMAL(19,2)}, que es una precision que hoy no
 * existe en el resto del esquema -las 38 columnas monetarias actuales son
 * {@code DECIMAL(12,2)}-. Es el choque C1 de {@code suscripciones-modelo.md} y
 * gana la especificacion: ningun {@code JOIN} cruza un importe de suscripciones
 * con uno de facturacion DIAN.
 *
 * <p>
 * {@code price_list_id} y {@code catalog_item_id} son columnas sueltas y no
 * asociaciones: las FK viven en el esquema, y esta feature no necesita el grafo
 * de objetos ni de su propia cabecera ni del articulo. Sin {@code @ManyToOne}
 * no hay N+1 que evitar ni {@code @EntityGraph} que mantener.
 */
@Entity
@Table(name = "catalog_prices")
@SQLDelete(sql = "UPDATE catalog_prices SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class CatalogPriceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price_list_id", nullable = false)
    private Long priceListId;

    @Column(name = "catalog_item_id", nullable = false)
    private Long catalogItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "tier_min", nullable = false)
    private int tierMin;

    @Column(name = "tier_max")
    private Integer tierMax;

    @Column(name = "included_quantity", nullable = false)
    private int includedQuantity;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitAmount;

    @Column(name = "setup_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal setupAmount;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 20)
    private TaxTreatment taxTreatment;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CatalogPriceJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public Long getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(Long catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public int getTierMin() {
        return tierMin;
    }

    public void setTierMin(int tierMin) {
        this.tierMin = tierMin;
    }

    public Integer getTierMax() {
        return tierMax;
    }

    public void setTierMax(Integer tierMax) {
        this.tierMax = tierMax;
    }

    public int getIncludedQuantity() {
        return includedQuantity;
    }

    public void setIncludedQuantity(int includedQuantity) {
        this.includedQuantity = includedQuantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(BigDecimal unitAmount) {
        this.unitAmount = unitAmount;
    }

    public BigDecimal getSetupAmount() {
        return setupAmount;
    }

    public void setSetupAmount(BigDecimal setupAmount) {
        this.setupAmount = setupAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public TaxTreatment getTaxTreatment() {
        return taxTreatment;
    }

    public void setTaxTreatment(TaxTreatment taxTreatment) {
        this.taxTreatment = taxTreatment;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
