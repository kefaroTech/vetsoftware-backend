package com.vetsoftware.app.companylimitevent.infrastructure.web.response;

import com.vetsoftware.app.companylimitevent.application.dto.UsageReconciliationDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lo que hizo una pasada del recuento periódico.
 *
 * <p>
 * <strong>Los cuatro números no son adorno</strong>: son lo único que permite
 * alertar sobre el propio recuento. {@code skipped} creciendo dice que hay ejes
 * vendidos que nadie puede comprobar, y {@code drifted} creciendo dice que algo
 * está perdiendo movimientos del contador, que es exactamente lo que R-LIMIT-30
 * existe para detectar. Un recuento que solo devolviera «ok» sería un indicador
 * de salud que no distingue entre estar sano y no haber mirado.
 *
 * <p>
 * {@code lastId} es el cursor con el que se pide el lote siguiente, y
 * {@code fullBatch} dice si hay más: va por cursor y no por prioridad porque un
 * contador con desvío no se sella y volvería a salir en todos los lotes.
 */
public record UsageReconciliationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int examined,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int matched,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int drifted,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int skipped,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cursor para pedir el lote siguiente") long lastId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "El lote salió lleno: hay que pedir otro") boolean fullBatch) {

    public static UsageReconciliationResponse from(UsageReconciliationDto dto, int batchSize) {
        return new UsageReconciliationResponse(dto.examined(), dto.matched(), dto.drifted(),
                dto.skipped(), dto.lastId(), dto.isFullBatch(batchSize));
    }
}
