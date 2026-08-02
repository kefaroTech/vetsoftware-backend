package com.vetsoftware.app.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asiento del kardex (append-only). {@code type}/{@code referenceType} se guardan como el name del
 * enum.
 */
@Entity
@Table(name = "stock_movement")
public class StockMovementJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "lot_id", nullable = false)
  private Long lotId;

  @Column(name = "type", nullable = false, length = 20)
  private String type;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
  private BigDecimal unitCost;

  @Column(name = "reference_type", nullable = false, length = 30)
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "reason", length = 255)
  private String reason;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_date", nullable = false)
  private LocalDateTime createdDate;

  protected StockMovementJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Long getLotId() {
    return lotId;
  }

  public void setLotId(Long lotId) {
    this.lotId = lotId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(String referenceType) {
    this.referenceType = referenceType;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(Long referenceId) {
    this.referenceId = referenceId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }
}
