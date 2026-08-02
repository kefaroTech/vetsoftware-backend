package com.vetsoftware.app.purchaseorder.domain;

/**
 * Estado de una orden de compra. Ciclo de vida: DRAFT (editable) → PLACED
 * (emitida al proveedor) → PARTIALLY_RECEIVED / RECEIVED (según lo que llega en
 * las recepciones de mercancía). CANCELLED es terminal y solo alcanzable desde
 * DRAFT o PLACED (nunca si ya se recibió algo).
 */
public enum PurchaseOrderStatus {
    DRAFT, PLACED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED
}
