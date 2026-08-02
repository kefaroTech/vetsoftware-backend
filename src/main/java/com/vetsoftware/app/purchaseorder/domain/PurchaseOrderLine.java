package com.vetsoftware.app.purchaseorder.domain;

import java.math.BigDecimal;

/**
 * Value object dentro del agregado {@link PurchaseOrder}: una línea de producto pedido. Es mutable
 * en el único aspecto que evoluciona tras emitir la orden — {@code quantityReceived}, que crece con
 * cada recepción de mercancía. El resto de campos (producto, cantidad pedida, costo) son inmutables
 * una vez creada la línea.
 */
public class PurchaseOrderLine {
  private final Long id;
  private final ProductRef product;
  private final int quantityOrdered;
  private final BigDecimal unitCost;
  private int quantityReceived;

  public PurchaseOrderLine(
      Long id, ProductRef product, int quantityOrdered, BigDecimal unitCost, int quantityReceived) {
    validate(product, quantityOrdered, unitCost, quantityReceived);
    this.id = id;
    this.product = product;
    this.quantityOrdered = quantityOrdered;
    this.unitCost = unitCost;
    this.quantityReceived = quantityReceived;
  }

  public static PurchaseOrderLine create(
      ProductRef product, int quantityOrdered, BigDecimal unitCost) {
    return new PurchaseOrderLine(null, product, quantityOrdered, unitCost, 0);
  }

  private static void validate(
      ProductRef product, int quantityOrdered, BigDecimal unitCost, int quantityReceived) {
    if (product == null) throw new IllegalArgumentException("line product is required");
    if (quantityOrdered <= 0)
      throw new IllegalArgumentException("quantityOrdered must be greater than zero");
    if (unitCost == null) throw new IllegalArgumentException("unitCost is required");
    if (unitCost.signum() < 0) throw new IllegalArgumentException("unitCost cannot be negative");
    if (quantityReceived < 0)
      throw new IllegalArgumentException("quantityReceived cannot be negative");
  }

  /** Cantidad aún pendiente de recibir (pedida menos recibida). */
  public int pendingQuantity() {
    return quantityOrdered - quantityReceived;
  }

  /** ¿Se recibió ya la totalidad de lo pedido en esta línea? */
  public boolean isFullyReceived() {
    return quantityReceived >= quantityOrdered;
  }

  /** Incrementa lo recibido; rechaza cantidades no positivas o que excedan lo pendiente. */
  void receive(int quantity) {
    if (quantity <= 0)
      throw new IllegalArgumentException("received quantity must be greater than zero");
    if (quantity > pendingQuantity()) {
      throw new IllegalArgumentException(
          "received quantity " + quantity + " exceeds pending quantity " + pendingQuantity());
    }
    this.quantityReceived += quantity;
  }

  /** Revierte lo recibido (reversa de una recepción); nunca baja de 0. */
  void revertReceive(int quantity) {
    if (quantity <= 0)
      throw new IllegalArgumentException("reverted quantity must be greater than zero");
    this.quantityReceived = Math.max(0, this.quantityReceived - quantity);
  }

  public Long getId() {
    return id;
  }

  public ProductRef getProduct() {
    return product;
  }

  public int getQuantityOrdered() {
    return quantityOrdered;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public int getQuantityReceived() {
    return quantityReceived;
  }
}
