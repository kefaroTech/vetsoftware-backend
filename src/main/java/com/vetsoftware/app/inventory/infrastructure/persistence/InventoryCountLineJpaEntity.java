package com.vetsoftware.app.inventory.infrastructure.persistence;

import jakarta.persistence.*;

/** Línea de un conteo físico: sistema vs contado y su diferencia (persistida para reporte SQL). */
@Entity
@Table(name = "inventory_count_line")
public class InventoryCountLineJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "count_id", nullable = false)
  private InventoryCountJpaEntity count;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "system_quantity", nullable = false)
  private int systemQuantity;

  @Column(name = "counted_quantity", nullable = false)
  private int countedQuantity;

  @Column(name = "difference", nullable = false)
  private int difference;

  protected InventoryCountLineJpaEntity() {}

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public InventoryCountJpaEntity getCount() {
    return count;
  }

  public void setCount(InventoryCountJpaEntity count) {
    this.count = count;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public int getSystemQuantity() {
    return systemQuantity;
  }

  public void setSystemQuantity(int systemQuantity) {
    this.systemQuantity = systemQuantity;
  }

  public int getCountedQuantity() {
    return countedQuantity;
  }

  public void setCountedQuantity(int countedQuantity) {
    this.countedQuantity = countedQuantity;
  }

  public int getDifference() {
    return difference;
  }

  public void setDifference(int difference) {
    this.difference = difference;
  }
}
