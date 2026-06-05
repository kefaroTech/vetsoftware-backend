package com.vetsoftware.app.debtopenaccount.infrastructure.persistence;

import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "debt_open_accounts")
@SQLDelete(sql = "UPDATE debt_open_accounts SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class DebtOpenAccountJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

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

    protected DebtOpenAccountJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public OpenAccountJpaEntity getOpenAccount() { return openAccount; }
    public void setOpenAccount(OpenAccountJpaEntity openAccount) { this.openAccount = openAccount; }
    public EmployeeJpaEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(EmployeeJpaEntity createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
