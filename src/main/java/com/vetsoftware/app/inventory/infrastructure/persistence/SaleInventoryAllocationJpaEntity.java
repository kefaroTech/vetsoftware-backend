package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.catalog.domain.SellableItemType;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.productbundle.infrastructure.persistence.ProductBundleJpaEntity;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Fotografía append-only de la descomposición comercial a inventario. La
 * definición vigente del combo nunca se usa para reintentar ni revertir una
 * venta histórica.
 */
@Entity
@Table(name = "sale_inventory_allocations")
public class SaleInventoryAllocationJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchJpaEntity branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private StockReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "commercial_line_key", nullable = false, length = 100)
    private String commercialLineKey;

    @Column(name = "component_sequence", nullable = false)
    private int componentSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SellableItemType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_presentation_id")
    private ProductPresentationJpaEntity sourcePresentation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_bundle_id")
    private ProductBundleJpaEntity sourceBundle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_presentation_id", nullable = false)
    private ProductPresentationJpaEntity componentPresentation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_product_id", nullable = false)
    private ProductJpaEntity componentProduct;

    @Column(name = "source_quantity", nullable = false)
    private int sourceQuantity;

    @Column(name = "component_quantity", nullable = false)
    private int componentQuantity;

    @Column(name = "conversion_factor", nullable = false)
    private int conversionFactor;

    @Column(name = "base_quantity", nullable = false)
    private int baseQuantity;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected SaleInventoryAllocationJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public BranchJpaEntity getBranch() {
        return branch;
    }

    public void setBranch(BranchJpaEntity branch) {
        this.branch = branch;
    }

    public StockReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(StockReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getCommercialLineKey() {
        return commercialLineKey;
    }

    public void setCommercialLineKey(String commercialLineKey) {
        this.commercialLineKey = commercialLineKey;
    }

    public int getComponentSequence() {
        return componentSequence;
    }

    public void setComponentSequence(int componentSequence) {
        this.componentSequence = componentSequence;
    }

    public SellableItemType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SellableItemType sourceType) {
        this.sourceType = sourceType;
    }

    public ProductPresentationJpaEntity getSourcePresentation() {
        return sourcePresentation;
    }

    public void setSourcePresentation(ProductPresentationJpaEntity sourcePresentation) {
        this.sourcePresentation = sourcePresentation;
    }

    public ProductBundleJpaEntity getSourceBundle() {
        return sourceBundle;
    }

    public void setSourceBundle(ProductBundleJpaEntity sourceBundle) {
        this.sourceBundle = sourceBundle;
    }

    public ProductPresentationJpaEntity getComponentPresentation() {
        return componentPresentation;
    }

    public void setComponentPresentation(ProductPresentationJpaEntity componentPresentation) {
        this.componentPresentation = componentPresentation;
    }

    public ProductJpaEntity getComponentProduct() {
        return componentProduct;
    }

    public void setComponentProduct(ProductJpaEntity componentProduct) {
        this.componentProduct = componentProduct;
    }

    public int getSourceQuantity() {
        return sourceQuantity;
    }

    public void setSourceQuantity(int sourceQuantity) {
        this.sourceQuantity = sourceQuantity;
    }

    public int getComponentQuantity() {
        return componentQuantity;
    }

    public void setComponentQuantity(int componentQuantity) {
        this.componentQuantity = componentQuantity;
    }

    public int getConversionFactor() {
        return conversionFactor;
    }

    public void setConversionFactor(int conversionFactor) {
        this.conversionFactor = conversionFactor;
    }

    public int getBaseQuantity() {
        return baseQuantity;
    }

    public void setBaseQuantity(int baseQuantity) {
        this.baseQuantity = baseQuantity;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
