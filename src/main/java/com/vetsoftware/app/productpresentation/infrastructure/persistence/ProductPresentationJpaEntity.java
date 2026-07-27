package com.vetsoftware.app.productpresentation.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** SKU vendible de un producto y conversión entera a su unidad base de inventario. */
@Entity
@Table(name = "product_presentations")
@SQLDelete(sql = "UPDATE product_presentations SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class ProductPresentationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_measure_code", nullable = false)
    private UnitMeasureCatalogJpaEntity unitMeasure;

    @Column(name = "conversion_factor", nullable = false)
    private int conversionFactor;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "default_presentation", nullable = false)
    private boolean defaultPresentation;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean enabled = true;

    protected ProductPresentationJpaEntity() {}

    public static ProductPresentationJpaEntity create(
            CompanyJpaEntity company, ProductJpaEntity product, String name,
            UnitMeasureCatalogJpaEntity unitMeasure, int conversionFactor,
            BigDecimal salePrice, boolean defaultPresentation, Long actorId) {
        ProductPresentationJpaEntity entity = new ProductPresentationJpaEntity();
        entity.company = company;
        entity.product = product;
        entity.apply(name, unitMeasure, conversionFactor, salePrice, defaultPresentation, actorId);
        entity.createdDate = LocalDateTime.now();
        return entity;
    }

    public void update(String name, UnitMeasureCatalogJpaEntity unitMeasure,
                       int conversionFactor, BigDecimal salePrice,
                       boolean defaultPresentation, Long actorId) {
        apply(name, unitMeasure, conversionFactor, salePrice, defaultPresentation, actorId);
    }

    public void markDefault(boolean value, Long actorId) {
        this.defaultPresentation = value;
        this.updatedBy = actorId;
        this.updatedDate = LocalDateTime.now();
    }

    private void apply(String name, UnitMeasureCatalogJpaEntity unitMeasure,
                       int conversionFactor, BigDecimal salePrice,
                       boolean defaultPresentation, Long actorId) {
        this.name = name;
        this.unitMeasure = unitMeasure;
        this.conversionFactor = conversionFactor;
        this.salePrice = salePrice;
        this.defaultPresentation = defaultPresentation;
        this.updatedBy = actorId;
        this.updatedDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public ProductJpaEntity getProduct() { return product; }
    public void setProduct(ProductJpaEntity product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UnitMeasureCatalogJpaEntity getUnitMeasure() { return unitMeasure; }
    public void setUnitMeasure(UnitMeasureCatalogJpaEntity unitMeasure) { this.unitMeasure = unitMeasure; }
    public int getConversionFactor() { return conversionFactor; }
    public void setConversionFactor(int conversionFactor) { this.conversionFactor = conversionFactor; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public boolean isDefaultPresentation() { return defaultPresentation; }
    public void setDefaultPresentation(boolean defaultPresentation) { this.defaultPresentation = defaultPresentation; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
