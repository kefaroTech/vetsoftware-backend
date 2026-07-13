package com.vetsoftware.app.inventory.infrastructure.persistence;

import jakarta.persistence.*;

/** Saldo materializado por (producto, sede). {@code version} para lock optimista; la lectura FOR UPDATE serializa. */
@Entity
@Table(name = "stock_balance", uniqueConstraints = {
    @UniqueConstraint(name = "uq_stock_balance_product_branch", columnNames = {"product_id", "branch_id"})
})
public class StockBalanceJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StockBalanceJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
