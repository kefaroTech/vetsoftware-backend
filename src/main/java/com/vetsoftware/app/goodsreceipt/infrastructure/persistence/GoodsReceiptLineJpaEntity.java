package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "goods_receipt_lines")
public class GoodsReceiptLineJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "goods_receipt_id", nullable = false)
  private GoodsReceiptJpaEntity goodsReceipt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private ProductJpaEntity product;

  // Enlace por id a la línea de la orden de compra (columna simple, sin FK JPA). Null en recepción
  // directa.
  @Column(name = "purchase_order_line_id")
  private Long purchaseOrderLineId;

  @Column(name = "lot_number", length = 60)
  private String lotNumber;

  @Column(name = "expire_date")
  private LocalDate expireDate;

  @Column(name = "quantity_received", nullable = false)
  private int quantityReceived;

  @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitCost;

  protected GoodsReceiptLineJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public GoodsReceiptJpaEntity getGoodsReceipt() {
    return goodsReceipt;
  }

  public void setGoodsReceipt(GoodsReceiptJpaEntity goodsReceipt) {
    this.goodsReceipt = goodsReceipt;
  }

  public ProductJpaEntity getProduct() {
    return product;
  }

  public void setProduct(ProductJpaEntity product) {
    this.product = product;
  }

  public Long getPurchaseOrderLineId() {
    return purchaseOrderLineId;
  }

  public void setPurchaseOrderLineId(Long purchaseOrderLineId) {
    this.purchaseOrderLineId = purchaseOrderLineId;
  }

  public String getLotNumber() {
    return lotNumber;
  }

  public void setLotNumber(String lotNumber) {
    this.lotNumber = lotNumber;
  }

  public LocalDate getExpireDate() {
    return expireDate;
  }

  public void setExpireDate(LocalDate expireDate) {
    this.expireDate = expireDate;
  }

  public int getQuantityReceived() {
    return quantityReceived;
  }

  public void setQuantityReceived(int quantityReceived) {
    this.quantityReceived = quantityReceived;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }
}
