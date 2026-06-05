package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "general_charge_open_accounts")
@SQLDelete(sql = "UPDATE general_charge_open_accounts SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class GeneralChargeOpenAccountJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "unit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_id", nullable = true)
    private TaxJpaEntity tax;

    @Column(name = "has_tax", nullable = false)
    private boolean hasTax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "open_account_id", nullable = false)
    private OpenAccountJpaEntity openAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private EmployeeJpaEntity createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected GeneralChargeOpenAccountJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getUnitAmount() { return unitAmount; }
    public void setUnitAmount(BigDecimal unitAmount) { this.unitAmount = unitAmount; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public TaxJpaEntity getTax() { return tax; }
    public void setTax(TaxJpaEntity tax) { this.tax = tax; }
    public boolean isHasTax() { return hasTax; }
    public void setHasTax(boolean hasTax) { this.hasTax = hasTax; }
    public OpenAccountJpaEntity getOpenAccount() { return openAccount; }
    public void setOpenAccount(OpenAccountJpaEntity openAccount) { this.openAccount = openAccount; }
    public EmployeeJpaEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(EmployeeJpaEntity createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
