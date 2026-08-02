package com.vetsoftware.app.goodsreceipt.domain;

/**
 * Estado de una recepción de mercancía. DRAFT es editable; CONFIRMED afecta el inventario;
 * CANCELLED lo revierte.
 */
public enum GoodsReceiptStatus {
  DRAFT,
  CONFIRMED,
  CANCELLED
}
