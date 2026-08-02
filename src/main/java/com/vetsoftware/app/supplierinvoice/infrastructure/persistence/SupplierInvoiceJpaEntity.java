package com.vetsoftware.app.supplierinvoice.infrastructure.persistence;

import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.supplier.infrastructure.persistence.SupplierJpaEntity;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Factura de proveedor / cuenta por pagar (cabecera). {@code purchase_order_id} y {@code
 * goods_receipt_id} son columnas Long peladas (enlace por id, sin @ManyToOne a esas features, para
 * no acoplar el modelo JPA). Los abonos cuelgan por cascade + orphanRemoval.
 */
@Entity
@Table(name = "supplier_invoices")
@SQLDelete(sql = "UPDATE supplier_invoices SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class SupplierInvoiceJpaEntity {
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

  @Column(name = "purchase_order_id")
  private Long purchaseOrderId;

  @Column(name = "goods_receipt_id")
  private Long goodsReceiptId;

  @Column(name = "invoice_number", nullable = false, length = 60)
  private String invoiceNumber;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal taxAmount;

  @Column(name = "withholding_amount", nullable = false, precision = 14, scale = 2)
  private BigDecimal withholdingAmount;

  @Column(name = "total", nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SupplierInvoiceStatus status;

  @Column(name = "notes", length = 500)
  private String notes;

  @OneToMany(
      mappedBy = "invoice",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<SupplierInvoicePaymentJpaEntity> payments = new ArrayList<>();

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

  protected SupplierInvoiceJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CompanyJpaEntity getCompany() {
    return company;
  }

  public void setCompany(CompanyJpaEntity company) {
    this.company = company;
  }

  public BranchJpaEntity getBranch() {
    return branch;
  }

  public void setBranch(BranchJpaEntity branch) {
    this.branch = branch;
  }

  public SupplierJpaEntity getSupplier() {
    return supplier;
  }

  public void setSupplier(SupplierJpaEntity supplier) {
    this.supplier = supplier;
  }

  public Long getPurchaseOrderId() {
    return purchaseOrderId;
  }

  public void setPurchaseOrderId(Long purchaseOrderId) {
    this.purchaseOrderId = purchaseOrderId;
  }

  public Long getGoodsReceiptId() {
    return goodsReceiptId;
  }

  public void setGoodsReceiptId(Long goodsReceiptId) {
    this.goodsReceiptId = goodsReceiptId;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public LocalDate getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(LocalDate issueDate) {
    this.issueDate = issueDate;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public void setSubtotal(BigDecimal subtotal) {
    this.subtotal = subtotal;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(BigDecimal taxAmount) {
    this.taxAmount = taxAmount;
  }

  public BigDecimal getWithholdingAmount() {
    return withholdingAmount;
  }

  public void setWithholdingAmount(BigDecimal withholdingAmount) {
    this.withholdingAmount = withholdingAmount;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public void setTotal(BigDecimal total) {
    this.total = total;
  }

  public SupplierInvoiceStatus getStatus() {
    return status;
  }

  public void setStatus(SupplierInvoiceStatus status) {
    this.status = status;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public List<SupplierInvoicePaymentJpaEntity> getPayments() {
    return payments;
  }

  public void setPayments(List<SupplierInvoicePaymentJpaEntity> payments) {
    this.payments = payments;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(LocalDateTime updatedDate) {
    this.updatedDate = updatedDate;
  }

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** Añade un abono manteniendo el back-reference para el cascade persist. */
  public void addPayment(SupplierInvoicePaymentJpaEntity payment) {
    payment.setInvoice(this);
    this.payments.add(payment);
  }
}
