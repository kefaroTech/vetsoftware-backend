package com.vetsoftware.app.catalogbarcode.infrastructure.persistence;

import com.vetsoftware.app.catalog.domain.SellableItemType;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Registro central que garantiza que un barcode activo resuelve un único artículo por empresa. */
@Entity
@Table(name = "catalog_barcodes")
@SQLDelete(sql = "UPDATE catalog_barcodes SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class CatalogBarcodeJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(nullable = false, length = 64)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private SellableItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentation_id")
    private ProductPresentationJpaEntity presentation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id")
    private ProductBundleJpaEntity bundle;

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

    protected CatalogBarcodeJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public SellableItemType getItemType() { return itemType; }
    public void setItemType(SellableItemType itemType) { this.itemType = itemType; }
    public ProductPresentationJpaEntity getPresentation() { return presentation; }
    public void setPresentation(ProductPresentationJpaEntity presentation) { this.presentation = presentation; }
    public ProductBundleJpaEntity getBundle() { return bundle; }
    public void setBundle(ProductBundleJpaEntity bundle) { this.bundle = bundle; }
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
