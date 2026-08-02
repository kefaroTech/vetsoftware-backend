package com.vetsoftware.app.inventory.domain;

/**
 * Una línea de un conteo físico: cuánto había en el sistema para un producto ({@code
 * systemQuantity}) frente a cuánto se contó físicamente ({@code countedQuantity}) en la sede de la
 * sesión. La {@code difference} (conteo − sistema) es el ajuste que hay que aplicar: positiva ⇒
 * sobrante (ADJUSTMENT_IN), negativa ⇒ faltante (ADJUSTMENT_OUT), cero ⇒ ya cuadra.
 */
public class InventoryCountLine {
  private Long id;
  private final Long productId;
  private final int systemQuantity;
  private final int countedQuantity;

  public InventoryCountLine(Long id, Long productId, int systemQuantity, int countedQuantity) {
    if (productId == null) throw new IllegalArgumentException("productId is required");
    if (countedQuantity < 0)
      throw new IllegalArgumentException("countedQuantity cannot be negative");
    this.id = id;
    this.productId = productId;
    this.systemQuantity = systemQuantity;
    this.countedQuantity = countedQuantity;
  }

  public static InventoryCountLine create(Long productId, int systemQuantity, int countedQuantity) {
    return new InventoryCountLine(null, productId, systemQuantity, countedQuantity);
  }

  /** Ajuste a aplicar = contado − sistema. Positiva sobra, negativa falta. */
  public int difference() {
    return countedQuantity - systemQuantity;
  }

  public Long getId() {
    return id;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  public Long getProductId() {
    return productId;
  }

  public int getSystemQuantity() {
    return systemQuantity;
  }

  public int getCountedQuantity() {
    return countedQuantity;
  }
}
