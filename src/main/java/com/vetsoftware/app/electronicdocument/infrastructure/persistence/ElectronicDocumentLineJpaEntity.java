package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.domain.TaxCategory;
import com.vetsoftware.app.electronicdocument.domain.TaxScheme;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "electronic_document_lines")
public class ElectronicDocumentLineJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "electronic_document_id", nullable = false)
  private ElectronicDocumentJpaEntity document;

  @Column(name = "line_number", nullable = false)
  private int lineNumber;

  @Column(name = "description", nullable = false, length = 255)
  private String description;

  @Column(name = "quantity", nullable = false, precision = 12, scale = 2)
  private BigDecimal quantity;

  @Column(name = "unit_measure_code", nullable = false, length = 10)
  private String unitMeasureCode;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "line_extension_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal lineExtensionAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_category", nullable = false, length = 20)
  private TaxCategory taxCategory;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_scheme", length = 20)
  private TaxScheme taxScheme;

  @Column(name = "tax_rate", precision = 5, scale = 2)
  private BigDecimal taxRate;

  @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal taxAmount;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  protected ElectronicDocumentLineJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ElectronicDocumentJpaEntity getDocument() {
    return document;
  }

  public void setDocument(ElectronicDocumentJpaEntity document) {
    this.document = document;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public String getUnitMeasureCode() {
    return unitMeasureCode;
  }

  public void setUnitMeasureCode(String unitMeasureCode) {
    this.unitMeasureCode = unitMeasureCode;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getLineExtensionAmount() {
    return lineExtensionAmount;
  }

  public void setLineExtensionAmount(BigDecimal lineExtensionAmount) {
    this.lineExtensionAmount = lineExtensionAmount;
  }

  public TaxCategory getTaxCategory() {
    return taxCategory;
  }

  public void setTaxCategory(TaxCategory taxCategory) {
    this.taxCategory = taxCategory;
  }

  public TaxScheme getTaxScheme() {
    return taxScheme;
  }

  public void setTaxScheme(TaxScheme taxScheme) {
    this.taxScheme = taxScheme;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public BigDecimal getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(BigDecimal taxAmount) {
    this.taxAmount = taxAmount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }
}
