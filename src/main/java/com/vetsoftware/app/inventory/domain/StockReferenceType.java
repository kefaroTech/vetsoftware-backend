package com.vetsoftware.app.inventory.domain;

/** Origen del movimiento de inventario (para trazabilidad, idempotencia y compensación). */
public enum StockReferenceType {
  POS_DOCUMENT,
  OPEN_ACCOUNT_CHARGE,
  GOODS_RECEIPT,
  ADJUSTMENT,
  TRANSFER,
  CLINICAL_EVENT
}
