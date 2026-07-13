package com.vetsoftware.app.inventory.application.dto;

import com.vetsoftware.app.inventory.domain.InventoryCountLine;

/** Salida de una línea de conteo: sistema vs contado y la diferencia (ajuste) resultante. */
public record InventoryCountLineView(Long productId, int systemQuantity, int countedQuantity, int difference) {

    public static InventoryCountLineView from(InventoryCountLine line) {
        return new InventoryCountLineView(line.getProductId(), line.getSystemQuantity(),
            line.getCountedQuantity(), line.difference());
    }
}
