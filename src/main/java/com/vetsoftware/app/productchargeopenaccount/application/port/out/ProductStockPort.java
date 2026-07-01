package com.vetsoftware.app.productchargeopenaccount.application.port.out;

/**
 * Ajusta el stock del catálogo de productos al cargar/anular una venta. El descuento es atómico y
 * permite stock negativo (decisión de negocio: no se bloquea la venta por falta de stock registrado).
 */
public interface ProductStockPort {
    /** Descuenta {@code quantity} unidades del stock del producto de la empresa. */
    void decreaseStock(Long productId, Long companyId, int quantity);

    /** Repone {@code quantity} unidades (p. ej. al anular un cargo). */
    void increaseStock(Long productId, Long companyId, int quantity);
}
