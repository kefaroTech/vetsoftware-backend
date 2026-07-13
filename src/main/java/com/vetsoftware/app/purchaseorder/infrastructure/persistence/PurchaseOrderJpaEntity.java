package com.vetsoftware.app.purchaseorder.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderStatus;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "purchase_orders")
@SQLDelete(sql = "UPDATE purchase_orders SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class PurchaseOrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchJpaEntity branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierJpaEntity supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(length = 500)
    private String notes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private List<PurchaseOrderLineJpaEntity> lines = new ArrayList<>();

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected PurchaseOrderJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public BranchJpaEntity getBranch() { return branch; }
    public void setBranch(BranchJpaEntity branch) { this.branch = branch; }
    public SupplierJpaEntity getSupplier() { return supplier; }
    public void setSupplier(SupplierJpaEntity supplier) { this.supplier = supplier; }
    public PurchaseOrderStatus getStatus() { return status; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<PurchaseOrderLineJpaEntity> getLines() { return lines; }
    public void setLines(List<PurchaseOrderLineJpaEntity> lines) {
        this.lines.clear();
        if (lines != null) this.lines.addAll(lines);
    }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
