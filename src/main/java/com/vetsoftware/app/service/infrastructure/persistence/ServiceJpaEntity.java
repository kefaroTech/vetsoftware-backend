package com.vetsoftware.app.service.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.service.domain.TaxTreatment;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "services")
@SQLDelete(sql = "UPDATE services SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class ServiceJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 20)
    private TaxTreatment taxTreatment;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id", nullable = false)
    private ServiceCategoryJpaEntity serviceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_id", nullable = true)
    private TaxJpaEntity tax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ServiceJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public TaxTreatment getTaxTreatment() { return taxTreatment; }
    public void setTaxTreatment(TaxTreatment taxTreatment) { this.taxTreatment = taxTreatment; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public ServiceCategoryJpaEntity getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(ServiceCategoryJpaEntity serviceCategory) { this.serviceCategory = serviceCategory; }
    public TaxJpaEntity getTax() { return tax; }
    public void setTax(TaxJpaEntity tax) { this.tax = tax; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
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
