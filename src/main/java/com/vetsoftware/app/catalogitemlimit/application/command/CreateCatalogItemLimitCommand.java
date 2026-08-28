package com.vetsoftware.app.catalogitemlimit.application.command;

import com.vetsoftware.app.catalogitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.catalogitemlimit.domain.LimitMode;
import com.vetsoftware.app.catalogitemlimit.domain.ResetPeriod;
import java.math.BigDecimal;

/**
 * Declarar el techo de fábrica de un artículo sobre un eje.
 *
 * <p>
 * No lleva {@code measureKind}: se resuelve desde el eje. Aceptarlo de fuera
 * permitiría declarar un tipo distinto del real, y aunque la clave foránea
 * compuesta lo mataría en el motor, el error saldría a mitad de una operación
 * de catálogo sin decir qué corregir.
 */
public record CreateCatalogItemLimitCommand(Long catalogItemId, Long limitDimensionId,
        LimitMode mode, Integer limitQuantity, ResetPeriod resetPeriod,
        LimitEnforcement enforcement, BigDecimal overageUnitAmount, int warnThreshold,
        LimitMode trialMode, Integer trialLimitQuantity) {
}
