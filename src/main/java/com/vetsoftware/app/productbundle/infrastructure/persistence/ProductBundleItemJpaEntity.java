package com.vetsoftware.app.productbundle.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.productpresentation.infrastructure.persistence.ProductPresentationJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Componente vigente de un combo. La FK compuesta en BD impide mezclar empresas. */
@Entity
@Table(name = "product_bundle_items")
public class ProductBundleItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_id", nullable = false)
    private ProductBundleJpaEntity bundle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentation_id", nullable = false)
    private ProductPresentationJpaEntity presentation;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected ProductBundleItemJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public ProductBundleJpaEntity getBundle() { return bundle; }
    public void setBundle(ProductBundleJpaEntity bundle) { this.bundle = bundle; }
    public ProductPresentationJpaEntity getPresentation() { return presentation; }
    public void setPresentation(ProductPresentationJpaEntity presentation) { this.presentation = presentation; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
