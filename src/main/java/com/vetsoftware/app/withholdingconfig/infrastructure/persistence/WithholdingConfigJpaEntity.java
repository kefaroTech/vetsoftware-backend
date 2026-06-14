package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withholding_configs")
@SQLDelete(sql = "UPDATE withholding_configs SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class WithholdingConfigJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private CompanyJpaEntity company;

    @Column(name = "rete_fuente_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal reteFuenteRate;

    @Column(name = "rete_iva_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal reteIvaRate;

    @Column(name = "rete_ica_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal reteIcaRate;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected WithholdingConfigJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public BigDecimal getReteFuenteRate() { return reteFuenteRate; }
    public void setReteFuenteRate(BigDecimal reteFuenteRate) { this.reteFuenteRate = reteFuenteRate; }
    public BigDecimal getReteIvaRate() { return reteIvaRate; }
    public void setReteIvaRate(BigDecimal reteIvaRate) { this.reteIvaRate = reteIvaRate; }
    public BigDecimal getReteIcaRate() { return reteIcaRate; }
    public void setReteIcaRate(BigDecimal reteIcaRate) { this.reteIcaRate = reteIcaRate; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
