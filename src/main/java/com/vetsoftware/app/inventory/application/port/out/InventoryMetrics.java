package com.vetsoftware.app.inventory.application.port.out;

import com.vetsoftware.app.inventory.domain.StockMovementType;

/** Telemetría agregada del kardex. Nunca recibe empresa, sede, producto ni referencia. */
public interface InventoryMetrics {

    void movement(StockMovementType movementType, Result result, int units);

    enum Result {
        SUCCESS("success"),
        INSUFFICIENT_STOCK("insufficient_stock"),
        DUPLICATE_IGNORED("duplicate_ignored"),
        VALIDATION_ERROR("validation_error");

        private final String value;

        Result(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
