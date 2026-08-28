package com.vetsoftware.app.companylimitevent.application.dto;

/**
 * Lo que hizo una pasada del recuento.
 *
 * <p>
 * Los cuatro numeros no son adorno: son lo unico que permite alertar sobre el
 * propio recuento. {@code skipped} creciendo dice que hay ejes vendidos que
 * nadie puede comprobar --el hueco de {@code RealUsageCountPort}--, y
 * {@code drifted} creciendo dice que algo esta perdiendo movimientos del
 * contador, que es exactamente lo que R-LIMIT-30 existe para detectar.
 */
public record UsageReconciliationDto(int examined, int matched, int drifted, int skipped,
        long lastId) {

    /** {@code true} si el lote salio lleno: hay que pedir otro. */
    public boolean isFullBatch(int batchSize) {
        return examined == batchSize;
    }
}
