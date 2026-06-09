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

    @Column(name = "tax_percentage", precision = 5, scale = 2)
    private BigDecimal taxPercentage;

    @Column(name = "tax_name", length = 100)
    private String taxName;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

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

    @Column(name = "voided", nullable = false)
    private boolean voided = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by_id")
    private EmployeeJpaEntity voidedBy;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 255)
    private String voidReason;

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
    public BigDecimal getTaxPercentage() { return taxPercentage; }
    public void setTaxPercentage(BigDecimal taxPercentage) { this.taxPercentage = taxPercentage; }
    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public OpenAccountJpaEntity getOpenAccount() { return openAccount; }
    public void setOpenAccount(OpenAccountJpaEntity openAccount) { this.openAccount = openAccount; }
    public EmployeeJpaEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(EmployeeJpaEntity createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
    public EmployeeJpaEntity getVoidedBy() { return voidedBy; }
    public void setVoidedBy(EmployeeJpaEntity voidedBy) { this.voidedBy = voidedBy; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
}
