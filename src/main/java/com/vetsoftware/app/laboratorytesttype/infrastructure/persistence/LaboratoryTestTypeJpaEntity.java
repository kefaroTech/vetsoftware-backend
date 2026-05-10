package com.vetsoftware.app.laboratorytesttype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "laboratory_test_types")
public class LaboratoryTestTypeJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyJpaEntity company;

    @Column(nullable = false)
    private Boolean general;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected LaboratoryTestTypeJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public Boolean getGeneral() { return general; }
    public void setGeneral(Boolean general) { this.general = general; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}
