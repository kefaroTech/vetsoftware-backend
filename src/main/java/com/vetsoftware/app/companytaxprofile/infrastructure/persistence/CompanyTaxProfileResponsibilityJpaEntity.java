package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "company_tax_profile_responsibilities")
@SQLDelete(sql = "UPDATE company_tax_profile_responsibilities SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class CompanyTaxProfileResponsibilityJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_tax_profile_id", nullable = false)
    private CompanyTaxProfileJpaEntity companyTaxProfile;

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CompanyTaxProfileResponsibilityJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompanyTaxProfileJpaEntity getCompanyTaxProfile() {
        return companyTaxProfile;
    }

    public void setCompanyTaxProfile(CompanyTaxProfileJpaEntity companyTaxProfile) {
        this.companyTaxProfile = companyTaxProfile;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
