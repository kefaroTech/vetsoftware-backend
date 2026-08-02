package com.vetsoftware.app.inventory.domain;

/**
 * Saldo materializado por {@code (producto, sede)} = suma de los lotes disponibles de esa sede. Es
 * el roll-up para lectura rápida y el punto de serialización de las salidas concurrentes (lock
 * optimista {@code version} + lectura {@code FOR UPDATE} en la capa de persistencia). El costo NO
 * vive aquí — vive en los lotes.
 */
public class StockBalance {
  private Long id;
  private final Long companyId;
  private final Long branchId;
  private final Long productId;
  private int quantity;
  private int minStock;
  private Long version;

  public StockBalance(
      Long id,
      Long companyId,
      Long branchId,
      Long productId,
      int quantity,
      int minStock,
      Long version) {
    if (companyId == null) throw new IllegalArgumentException("companyId is required");
    if (branchId == null) throw new IllegalArgumentException("branchId is required");
    if (productId == null) throw new IllegalArgumentException("productId is required");
    this.id = id;
    this.companyId = companyId;
    this.branchId = branchId;
    this.productId = productId;
    this.quantity = quantity;
    this.minStock = minStock;
    this.version = version;
  }

  public static StockBalance create(Long companyId, Long branchId, Long productId, int minStock) {
    return new StockBalance(null, companyId, branchId, productId, 0, minStock, 0L);
  }

  public void add(int quantity) {
    this.quantity += quantity;
  }

  public void subtract(int quantity) {
    this.quantity -= quantity;
  }

  public void setMinStock(int minStock) {
    if (minStock < 0) throw new IllegalArgumentException("minStock cannot be negative");
    this.minStock = minStock;
  }

  public Long getId() {
    return id;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public Long getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public int getMinStock() {
    return minStock;
  }

  public Long getVersion() {
    return version;
  }
}
