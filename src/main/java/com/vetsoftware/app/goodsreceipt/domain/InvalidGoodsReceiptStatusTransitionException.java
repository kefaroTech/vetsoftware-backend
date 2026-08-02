package com.vetsoftware.app.goodsreceipt.domain;

/**
 * Transición de estado no permitida (p.ej. editar/confirmar fuera de DRAFT, o cancelar algo que no
 * está CONFIRMED).
 */
public class InvalidGoodsReceiptStatusTransitionException extends RuntimeException {
  public InvalidGoodsReceiptStatusTransitionException(String message) {
    super(message);
  }

  public InvalidGoodsReceiptStatusTransitionException(
      GoodsReceiptStatus from, GoodsReceiptStatus to) {
    super("Cannot transition goods receipt from " + from + " to " + to);
  }
}
